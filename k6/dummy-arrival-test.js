import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter, Trend } from 'k6/metrics';
import {
    BASE, TARGET_URI,
    loginPool, adminLogin, loadTestReset, loadTestState, tagFromState,
    scrapeBuckets, quantileFromBuckets, bucketTotal,
    summaryFiles, parseDurationSec,
} from './lib/dummytalk.js';

/*
 * getDummy() open model 본측정 — c7g.large 재측정 캠페인의 실질적인 스트레스 테스트
 *
 * ── 왜 open model(constant-arrival-rate)인가 ─────────────────────────────────
 *   기존 dummy-spike-test.js 는 per-vu-iterations(VU당 1회 발사) — 총 요청 수 = VU 수, 유지 구간 없음.
 *   VU 기반(closed model)은 서버가 느려지면 VU 가 응답을 기다리느라 요청률이 같이 떨어진다(coordinated omission).
 *   그러면 큐 누적·포화점이 영원히 안 보인다. 도착률을 고정해야 "들어오는 양 > 나가는 양" 지점이 드러난다.
 *   k6 가 도착률을 못 지키면 dropped_iterations 가 오른다 — 그 자체가 "수락 계층이 밀렸다"는 첫 신호다.
 *
 *   역할 분리: 따닥 정합성 증명(Lost Update 0 여부)은 dummy-spike-test.js, 처리량·지연·포화 순서는 이 파일.
 *
 * ── 파라미터 (전부 -e 로 지정) ──────────────────────────────────────────────
 *   RATE       도착률 rps                                 (기본 50)   스위프: 50 / 100 / 200 / 300
 *   INTERVAL   같은 유저가 다시 뽑기까지 간격(초)          (기본 5)
 *   DURATION   지속 부하 시간                              (기본 3m)
 *   TAG        파일명 특이사항 (카멜케이스)                 (기본: 서버 설정에서 자동, 예 v3InterceptorVtCp10)
 *   MAX_VUS    상한선 — rate 유지를 위해 띄울 최대 VU        (기본 RATE×10)  초과 시 dropped_iterations
 *   RESET      setup 에서 /load-test/reset 호출             (기본 true)
 *   ADMIN_EMAIL / ADMIN_PASSWORD  리셋·Lost Update 판정용    (미지정 시 둘 다 건너뜀)
 *   RESULT_DIR 결과 저장 경로                               (기본 ../../dev_notes/DummyTalk/results)
 *   BASE_URL   대상                                         (기본 http://localhost:8080)
 *
 *   파생값: ACTIVE(동시 활성 유저) = RATE × INTERVAL.   300rps × 5s = 1,500명 → 그만큼 시딩돼 있어야 함
 *           LIMIT = 40 (setup 리셋이 테스트 유저를 구독자로 전환. 5s × 3m = 36회 < 40 이라 회차 완주)
 *
 * ── 유저 배정 규칙 ─────────────────────────────────────────────────────────
 *   userIdx = iterationInTest % ACTIVE  →  같은 유저가 정확히 INTERVAL 마다 재등장 (주기를 결정적으로 보장)
 *   round   = floor(iterationInTest / ACTIVE)
 *   round ≥ LIMIT 이면 다음 풀 조각으로 이동 (1번 소진 → ACTIVE+1번). 40 한도에선 발동하지 않는 안전장치.
 *
 * ── 응답 해석 ──────────────────────────────────────────────────────────────
 *   200  정상 뽑기.  400(DUMMY_4001) 한도 소진 = 설계 오류(유저 수/한도 재확인).
 *   429  인터셉터/분산락 거절 — open model 에선 "직전 요청이 INTERVAL(5s) 안에 안 끝났다" 는 뜻.
 *        즉 서버측 지연이 주기를 넘었다는 포화 신호. 429 가 오르기 시작하는 rate 가 실질 상한이다.
 *
 * ── 실행 예시 (레포 루트에서) ──────────────────────────────────────────────
 *   # 워밍업 (집계 제외)
 *   k6 run -e BASE_URL=https://ddotg.dev -e ADMIN_EMAIL=... -e ADMIN_PASSWORD=... -e RATE=50 -e DURATION=2m -e TAG=warmup k6/dummy-arrival-test.js
 *   # 본회차 — setup 이 자동 리셋
 *   k6 run -e BASE_URL=https://ddotg.dev -e ADMIN_EMAIL=... -e ADMIN_PASSWORD=... -e RATE=100 k6/dummy-arrival-test.js
 */

// ─── 파라미터 ─────────────────────────────────────────────────────────────────
const RATE       = Number(__ENV.RATE || 50);
const INTERVAL   = Number(__ENV.INTERVAL || 5);
const DURATION   = __ENV.DURATION || '3m';
const ACTIVE     = Math.max(1, Math.round(RATE * INTERVAL));
const LIMIT      = 40; // 구독자 일일 한도 (DummyService: isSubscribe ? 40 : 20)
const PRE_VUS    = Math.max(50, RATE);
const MAX_VUS    = Math.max(PRE_VUS, Number(__ENV.MAX_VUS || RATE * 10));
const DO_RESET   = (__ENV.RESET || 'true') !== 'false';

// 회차 안에서 유저 한 명이 보내는 요청 수 → 한도를 넘기면 풀을 몇 조각으로 나눠 교체할지
const REQS_PER_USER = Math.ceil(parseDurationSec(DURATION) / INTERVAL);
const POOL_SLICES   = Math.max(1, Math.ceil(REQS_PER_USER / LIMIT));
const POOL_SIZE     = ACTIVE * POOL_SLICES;

// ─── 커스텀 메트릭 ────────────────────────────────────────────────────────────
const dummyDuration  = new Trend('dummy_req_duration_ms', true);   // 뽑기 응답 시간 (k6 체감, RTT 포함)
const rejectDuration = new Trend('reject_429_duration_ms', true);  // 거절 경로만의 응답 시간 = 거절 비용
const successCount   = new Counter('success_200_count');
const rejectCount    = new Counter('reject_429_count');
const limitHitCount  = new Counter('limit_hit_400_count');          // 0 이어야 정상 — 발생 시 유저 수/한도 설계 오류
const otherCount     = new Counter('unexpected_status_count');      // 5xx / 타임아웃 등
const raceSuspect    = new Counter('race_condition_suspect');       // remainingCount 음수 or 한도 초과
const lostUpdate     = new Counter('lost_update_count');            // teardown: success_200 − Δ(DB reqCount 합)
const droppedByPool  = new Counter('token_missing_count');          // 로그인 실패 유저에 배정된 반복

// ─── 시나리오 ─────────────────────────────────────────────────────────────────
export const options = {
    scenarios: {
        arrival: {
            executor: 'constant-arrival-rate',
            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: PRE_VUS,
            // maxVUs 는 preAllocatedVUs 보다 작을 수 없다 — k6 가 기동을 거부한다
            maxVUs: MAX_VUS,
        },
    },
    // setup 의 병렬 로그인(1,500명 ≈ 1~2분) + teardown 의 스크레이프 여유
    setupTimeout: '10m',
    teardownTimeout: '2m',
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    thresholds: {
        // 도착률을 못 지킨 반복 = 포화 표식. FAIL 이어도 회차는 유효하다 — "어디서부터 못 지켰나" 가 결과다.
        dropped_iterations:     ['count==0'],
        // 정합성 — 두 값은 반드시 0. 아니면 회차 전체를 의심할 것.
        race_condition_suspect: ['count<1'],
        lost_update_count:      ['count<1'],
        limit_hit_400_count:    ['count<1'],
    },
};

// ─── setup: 관리자 → 리셋 → 스냅샷 → 유저 풀 로그인 ───────────────────────────
export function setup() {
    console.log(`[setup] RATE=${RATE}/s INTERVAL=${INTERVAL}s DURATION=${DURATION} → ACTIVE=${ACTIVE}명, `
        + `유저당 ${REQS_PER_USER}회 (한도 ${LIMIT}), 풀 ${POOL_SIZE}명 (${POOL_SLICES}조각), maxVUs=${MAX_VUS}`);

    const adminToken = adminLogin();

    if (adminToken && DO_RESET) {
        const updated = loadTestReset(adminToken);
        console.log(`[setup] load-test/reset → ${updated}명 초기화 (reqCount=0, 구독자 전환)`);
    }

    const state = loadTestState(adminToken);
    if (state) {
        console.log(`[setup] 서버 설정: getDummy v${state.getDummyVersion}, interceptor=${state.interceptorEnabled}, `
            + `VT=${state.virtualThreads}, hikari=${state.hikariPoolSize}, 테스트 유저 ${state.testUserCount}명, reqCountSum=${state.reqCountSum}`);
        if (state.testUserCount < POOL_SIZE) {
            console.error(`[setup] ⚠️ 시딩된 유저 ${state.testUserCount}명 < 필요 ${POOL_SIZE}명 — TEST_LOAD_USERS_COUNT 를 올려 재기동할 것`);
        }
    }

    // 서버측 히스토그램 시작 스냅샷 — Micrometer 는 기동 이후 누적이라 회차값을 얻으려면 빼야 한다
    const buckets = scrapeBuckets(TARGET_URI);
    if (!buckets) console.warn('[setup] http_server_requests_seconds_bucket 없음 — percentiles-histogram 설정 확인');

    const pool = loginPool(POOL_SIZE);
    console.log(`[setup] 로그인 완료 ${Object.keys(pool.tokens).length}/${POOL_SIZE} (실패 ${pool.failed}) → 발사 시작`);

    return {
        adminToken,
        tokens: pool.tokens,
        base: { state, buckets },
    };
}

// ─── 메인: 반복 번호로 유저를 결정적으로 배정 ─────────────────────────────────
export default function (data) {
    const iter = exec.scenario.iterationInTest;          // 0부터 순차·유일 (로컬 실행)
    const round = Math.floor(iter / ACTIVE);             // 이 유저의 몇 번째 뽑기인가
    const slice = Math.min(POOL_SLICES - 1, Math.floor(round / LIMIT)); // 한도 소진 시 다음 조각으로
    const userNum = slice * ACTIVE + (iter % ACTIVE) + 1; // test{userNum}@test.com

    const token = data.tokens[String(userNum)];
    if (!token) { droppedByPool.add(1); return; }

    const res = http.get(`${BASE}${TARGET_URI}`, {
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        // 429/400 은 설계상 정상 응답 → http_req_failed 에서 제외
        responseCallback: http.expectedStatuses(200, 400, 429),
        // 포화 구간에서 Tomcat 큐 대기가 길어져도 데이터를 잃지 않도록 (k6 기본 60s 보다 짧게 잘라 회차가 늘어지는 것도 막음)
        timeout: '30s',
        tags: { name: 'getDummy' },
    });
    const elapsed = res.timings.duration;
    dummyDuration.add(elapsed);

    if (res.status === 429) {
        // 직전 요청이 INTERVAL 안에 안 끝남 → 서버측 지연 > 주기. 포화 신호로 카운트만 하고 재시도하지 않는다.
        rejectCount.add(1);
        rejectDuration.add(elapsed);
        return;
    }

    if (res.status === 400) {
        let code = null;
        try { code = res.json('code'); } catch (e) { /* noop */ }
        if (code === 'DUMMY_4001') {
            limitHitCount.add(1);
            if (iter % 100 === 0) console.warn(`[LIMIT] user=${userNum} round=${round} — 한도 소진, 설계 재확인`);
            return;
        }
        otherCount.add(1);
        return;
    }

    if (res.status !== 200) {
        otherCount.add(1);
        check(res, { 'getDummy 200': () => false });
        return;
    }

    successCount.add(1);
    check(res, { 'getDummy 200': () => true });

    let remaining;
    try { remaining = res.json('result.remainingCount'); } catch (e) { remaining = undefined; }
    if (remaining !== undefined && remaining !== null && (remaining < 0 || remaining > LIMIT)) {
        raceSuspect.add(1);
        console.warn(`[RACE SUSPECT] user=${userNum} iter=${iter} remaining=${remaining}`);
    }
}

// ─── teardown: 서버측 분위수 + Lost Update 판정 → handleSummary 로 넘길 텍스트 ───
// setup / teardown / handleSummary 는 같은 런타임에서 돌므로 모듈 변수로 전달할 수 있다 (VU 코드에서는 안 보임).
let processingReport = '';
let verdict = {};

export function teardown(data) {
    const base = data.base || {};
    const nowBuckets = scrapeBuckets(TARGET_URI);
    const q = (p) => quantileFromBuckets(base.buckets, nowBuckets, p);
    const fmt = (v) => (v === null || v === undefined ? 'n/a' : (v * 1000).toFixed(1) + 'ms');
    const serverP50 = q(0.5), serverP95 = q(0.95), serverP99 = q(0.99);
    const serverTotal = bucketTotal(base.buckets, nowBuckets);

    // Lost Update ground truth: DB 가 실제로 반영한 뽑기 수(Δ reqCount 합) vs k6 가 200 으로 센 수.
    // 같은 트랜잭션이 갱신을 덮어쓰면 Δ 가 success 보다 작다. (teardown 시점엔 success 카운터 값을 직접 읽을 수 없어
    // handleSummary 에서 metrics 로 최종 계산하고, 여기서는 서버 수치만 찍어 둔다)
    lostUpdate.add(0); // 표본을 하나 남겨 요약에 metric 이 나타나게 함 (실제 값은 handleSummary 에서 덮어씀)
    const now = loadTestState(data.adminToken);
    const deltaSum = (now && base.state) ? (now.reqCountSum - base.state.reqCountSum) : null;

    verdict = {
        server_p50_ms: serverP50 === null ? null : +(serverP50 * 1000).toFixed(1),
        server_p95_ms: serverP95 === null ? null : +(serverP95 * 1000).toFixed(1),
        server_p99_ms: serverP99 === null ? null : +(serverP99 * 1000).toFixed(1),
        server_handled_total: serverTotal,
        db_req_count_delta: deltaSum,
        config: base.state || null,
        config_after: now || null,
    };

    processingReport = `
────────────── 서버측 결과 (RATE ${RATE}/s · ${DURATION} · ACTIVE ${ACTIVE}명) ──────────────
  [서버 설정]      getDummy v${base.state ? base.state.getDummyVersion : '?'} · interceptor=${base.state ? base.state.interceptorEnabled : '?'}`
        + ` · VT=${base.state ? base.state.virtualThreads : '?'} · hikari=${base.state ? base.state.hikariPoolSize : '?'}
  [서버측 지연]    p50 / p95 / p99 = ${fmt(serverP50)} / ${fmt(serverP95)} / ${fmt(serverP99)}   (RTT 제외, ${TARGET_URI} 전 status 합산)
  [서버 처리 건수] ${serverTotal}건 (히스토그램 +Inf 델타)
  [DB 반영 건수]   Δ reqCount 합 = ${deltaSum === null ? 'n/a (관리자 미지정)' : deltaSum}
  ※ Lost Update 판정: 아래 METRICS 의 success_200_count 와 Δ reqCount 합이 같아야 한다. 작으면 갱신 유실.
  ※ k6 요약의 http_req_duration / dummy_req_duration_ms 는 RTT 를 포함한 '사용자 체감'. 전략 비교는 위 서버측 값으로.
  ※ reject_429_count 가 오르기 시작한 rate = 서버측 지연이 INTERVAL(${INTERVAL}s) 을 넘은 지점 = 실질 상한.
──────────────────────────────────────────────────────────────────────────`;
    console.log(processingReport);
}

// ─── handleSummary: Lost Update 최종 판정 + 파일 저장 ──────────────────────────
export function handleSummary(data) {
    const m = data.metrics || {};
    const cnt = (name) => (m[name] && m[name].values && m[name].values.count) || 0;
    const success = cnt('success_200_count');

    if (verdict.db_req_count_delta !== null && verdict.db_req_count_delta !== undefined) {
        verdict.lost_update_count = Math.max(0, success - verdict.db_req_count_delta);
        verdict.lost_update_ok = verdict.lost_update_count === 0;
        processingReport += `\n  [판정] success_200=${success} vs Δ reqCount=${verdict.db_req_count_delta} → `
            + (verdict.lost_update_ok ? 'Lost Update 없음 ✓' : `⚠️ ${verdict.lost_update_count}건 유실 의심`);
        // teardown 이후라 Counter 로는 못 올리므로 요약 metrics 에 직접 기록 (threshold 표시용)
        if (m.lost_update_count) {
            m.lost_update_count.values.count = verdict.lost_update_count;
            if (m.lost_update_count.thresholds && m.lost_update_count.thresholds['count<1']) {
                m.lost_update_count.thresholds['count<1'].ok = verdict.lost_update_ok;
            }
        }
    }
    verdict.success_200 = success;
    verdict.reject_429 = cnt('reject_429_count');
    verdict.limit_hit_400 = cnt('limit_hit_400_count');
    verdict.dropped_iterations = cnt('dropped_iterations');

    const tag = __ENV.TAG || tagFromState(verdict.config);
    return summaryFiles(data, {
        rate: RATE,
        duration: DURATION,
        tag,
        scenario: `open model (constant-arrival-rate ${RATE}/s ${DURATION}, INTERVAL ${INTERVAL}s, ACTIVE ${ACTIVE}명, maxVUs ${MAX_VUS})`,
        params: { rate: RATE, interval_s: INTERVAL, duration: DURATION, active_users: ACTIVE, pool_size: POOL_SIZE, max_vus: MAX_VUS, limit: LIMIT },
        processingReport,
        extra: verdict,
    });
}
