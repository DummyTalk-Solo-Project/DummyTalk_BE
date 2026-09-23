import http from 'k6/http';
import { check, group } from 'k6';
import { Counter, Trend, Gauge } from 'k6/metrics';
import {
    BASE, TARGET_URI,
    loginPool, adminLogin, loadTestReset, loadTestState, tagFromState,
    scrapeBuckets, quantileFromBuckets, bucketTotal,
    summaryFiles,
} from './lib/dummytalk.js';

/**
 * getDummy() 따닥 스파이크 — 동시성 전략의 **정합성** 판정용 (closed model)
 *
 * ── 이 파일의 역할 (dummy-arrival-test.js 와의 분담) ─────────────────────────
 *   arrival(open model) : 처리량·지연·포화 순서. 유저당 INTERVAL(5s) 간격이라
 *                         같은 유저의 요청이 겹치지 않는다 → Lost Update 가 원리상 안 나온다.
 *   spike(이 파일)      : 같은 유저에게 CONCURRENT 개의 VU 를 붙여 **동시에** 발사한다.
 *                         같은 info 행에 대한 동시 read-modify-write 를 만들어
 *                         "전략이 실제로 갱신 유실을 막는가" 를 판정한다.
 *
 * ── 왜 서버 상한을 다시 잴 필요가 없나 ──────────────────────────────────────
 *   이 테스트의 결론은 성능이 아니라 **불변식**이다:
 *     (1) success_200 == Δ reqCount (DB 가 실제로 반영한 수)  ← Lost Update ground truth
 *     (2) race_condition_suspect == 0                         ← remainingCount 이상값
 *     (3) 총 VU == success + 429 + limit                      ← 응답 분류 누락 없음
 *   참/거짓은 인스턴스 성능과 무관하다. 조정할 값은 총 VU 가 아니라 **CONCURRENT**(유저당 동시 요청 수)이고,
 *   총 VU 는 오히려 **서버를 포화시키지 않는 크기**여야 한다 — 포화시키면 큐·지연이 섞여 판정이 흐려진다.
 *
 * ── 시나리오 ────────────────────────────────────────────────────────────────
 *   per-vu-iterations(iterations=1) — VU 하나가 1회 발사 후 종료. 램프업·유지 구간 없음.
 *   VU → 유저 매핑: CONCURRENT 개의 VU 가 같은 유저를 공유 (따닥 재현의 핵심)
 *   setup 에서 로그인을 끝내므로 발사 시점에는 뽑기 요청만 남는다.
 *
 * ── 파라미터 ────────────────────────────────────────────────────────────────
 *   USERS       유저 수            (기본 200)   총 VU = USERS × CONCURRENT
 *   CONCURRENT  유저당 동시 요청 수 (기본 5)    ★ 따닥 강도. 이 값을 낮추면 재현이 안 된다
 *   TAG/STAGE   파일명 특이사항     (기본: 서버 설정에서 자동, 예 v1NoInterceptorPtCp10)
 *   BASE_URL / ADMIN_EMAIL / ADMIN_PASSWORD / RESULT_DIR — arrival 과 동일
 *
 * ── 응답 해석 ───────────────────────────────────────────────────────────────
 *   200  정상 뽑기
 *   429  V2 = @DistributedLock(waitTime=0) 획득 실패(CANT_GET_LOCK) / V3·V4 = 인터셉터 SETNX 거절
 *        → 둘 다 TOO_MANY_REQUESTS. 정상 동작이며 "따닥을 막았다" 는 증거다
 *   400  DUMMY_4001 한도 소진 (회차 전 리셋하므로 0 이어야 정상)
 *
 * ── 실행 예시 (레포 루트에서) ───────────────────────────────────────────────
 *   k6 run -e BASE_URL=https://ddotg.dev -e ADMIN_EMAIL=... -e ADMIN_PASSWORD=... \
 *          -e USERS=200 -e CONCURRENT=5 k6/dummy-spike-test.js
 */

// ─── 파라미터 ─────────────────────────────────────────────────────────────────
const USERS      = parseInt(__ENV.USERS)      || 200;
const CONCURRENT = parseInt(__ENV.CONCURRENT) || 5;
const TOTAL_VU   = USERS * CONCURRENT;
const LIMIT      = 40; // 리셋이 테스트 유저를 구독자로 만들므로 일일 한도 40 (DummyService: isSubscribe ? 40 : 20)

// ─── 커스텀 메트릭 ────────────────────────────────────────────────────────────
const dummyDuration  = new Trend('dummy_req_duration_ms', true);
// 거절 경로만의 응답 시간 = "거절 비용". V2(락, AOP 진입 후) vs V3(인터셉터, DB 진입 전) 비교 지표
const rejectDuration = new Trend('reject_429_duration_ms', true);
const successCount   = new Counter('success_200_count');
const rejectCount    = new Counter('reject_429_count');
const edge429Count   = new Counter('edge_429_count');          // Cloudflare 등 엣지 429 (앱 거절과 구분)
const limitHitCount  = new Counter('limit_hit_400_count');     // 한도 소진 — 리셋하므로 0 이어야 정상
const otherCount     = new Counter('unexpected_status_count'); // 5xx / 타임아웃
const raceSuspect    = new Counter('race_condition_suspect');  // remainingCount 가 범위를 벗어남
const lostUpdate     = new Counter('lost_update_count');       // handleSummary 에서 확정
const droppedByPool  = new Counter('token_missing_count');

// teardown -> handleSummary 는 별도 JS 런타임이라 모듈 변수가 전달되지 않는다. 값은 전부 메트릭으로 넘긴다
// (arrival 스크립트와 같은 이유·같은 방식 — lib/dummytalk.js 주석 참고)
const gServerP50      = new Gauge('server_p50_ms');
const gServerP95      = new Gauge('server_p95_ms');
const gServerP99      = new Gauge('server_p99_ms');
const gServerTotal    = new Gauge('server_handled_total');
const gDbDelta        = new Gauge('db_req_count_delta');
const gCfgVersion     = new Gauge('cfg_getdummy_version');
const gCfgInterceptor = new Gauge('cfg_interceptor_enabled');
const gCfgVt          = new Gauge('cfg_virtual_threads');
const gCfgPool        = new Gauge('cfg_hikari_pool_size');

// ─── 시나리오 ─────────────────────────────────────────────────────────────────
export const options = {
    scenarios: {
        ddotg_spike: {
            // per-vu-iterations: 각 VU 가 정확히 1번 실행 후 종료 -> 1회성 스파이크, 유지 구간 없음
            executor: 'per-vu-iterations',
            vus: TOTAL_VU,
            iterations: 1,
            maxDuration: '5m',
        },
    },
    // 엣지 개입 시 오리진 직타 폴백용 (arrival 과 동일). 공개 인증서 경로로 되돌릴 땐 -e STRICT_TLS=true
    insecureSkipTLSVerify: __ENV.STRICT_TLS !== 'true',
    setupTimeout: '10m',
    teardownTimeout: '2m',
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    thresholds: {
        // 정합성 — 전략이 제 역할을 했다면 둘 다 0. V1(보호 없음)에서만 깨지는 것이 기대값이다
        race_condition_suspect: ['count<1'],
        lost_update_count:      ['count<1'],
        // 회차 유효성 — 깨지면 그 회차는 해석 불가
        limit_hit_400_count:    ['count<1'],
        edge_429_count:         ['count<1'],
        token_missing_count:    ['count<1'],
    },
};

// ─── setup: 관리자 -> 리셋 -> 스냅샷 -> 유저 풀 로그인 ────────────────────────
export function setup() {
    console.log(`[setup] 따닥 스파이크 — 유저 ${USERS}명 × 동시 ${CONCURRENT} = 총 ${TOTAL_VU} VU (1회 발사)`);

    const adminToken = adminLogin();
    if (adminToken) {
        const updated = loadTestReset(adminToken);
        console.log(`[setup] load-test/reset → ${updated}명 초기화 (reqCount=0, 구독자 전환)`);
    }

    const state = loadTestState(adminToken);
    if (state) {
        console.log(`[setup] 서버 설정: getDummy v${state.getDummyVersion}, interceptor=${state.interceptorEnabled}, `
            + `VT=${state.virtualThreads}, hikari=${state.hikariPoolSize}, 테스트 유저 ${state.testUserCount}명`);
        if (state.testUserCount < USERS) {
            console.error(`[setup] 시딩된 유저 ${state.testUserCount}명 < 필요 ${USERS}명 — TEST_LOAD_USERS_COUNT 확인`);
        }
    }

    // 서버측 히스토그램 시작 스냅샷 (Micrometer 는 기동 이후 누적이라 회차값은 델타로 구한다)
    const buckets = scrapeBuckets(TARGET_URI);

    const pool = loginPool(USERS);
    console.log(`[setup] 로그인 완료 ${Object.keys(pool.tokens).length}/${USERS} (실패 ${pool.failed}) → 스파이크 발사`);

    return { adminToken, tokens: pool.tokens, base: { state, buckets } };
}

// ─── 메인: CONCURRENT 개의 VU 가 같은 유저로 동시에 1발 ───────────────────────
export default function (data) {
    // VU 1~CONCURRENT -> user1, VU CONCURRENT+1~2*CONCURRENT -> user2 ...
    const userNum = Math.floor((__VU - 1) / CONCURRENT) + 1;
    const token = data.tokens[String(userNum)];
    if (!token) { droppedByPool.add(1); return; }

    group('getDummy_ddotg', () => {
        const res = http.get(`${BASE}${TARGET_URI}`, {
            headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
            // 429/400 은 설계상 정상 응답 -> http_req_failed 에서 제외
            responseCallback: http.expectedStatuses(200, 400, 429),
            // 고 VU 스파이크에서 큐 대기가 길어져도 데이터를 잃지 않도록 (k6 기본 60s 보다 짧게 잘라 회차가 늘어지는 것도 방지)
            timeout: '30s',
            tags: { name: 'getDummy' },
        });
        const elapsed = res.timings.duration;
        dummyDuration.add(elapsed);

        if (res.status === 429) {
            // 앱 429 는 APIResponse JSON("code" 필드)을 담고, 엣지(Cloudflare) 429 는 HTML 이다.
            // 섞이면 "전략이 막았다" 와 "경로가 막았다" 를 구분할 수 없으므로 분리한다.
            const body = res.body || '';
            if (body.indexOf('"code"') < 0) { edge429Count.add(1); return; }
            rejectCount.add(1);
            rejectDuration.add(elapsed);
            check(res, { '따닥 차단 (429 정상)': () => true });
            return;
        }

        if (res.status === 400) {
            let code = null;
            try { code = res.json('code'); } catch (e) { /* noop */ }
            if (code === 'DUMMY_4001') { limitHitCount.add(1); return; }
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

        // 레이스 감지: 정상이라면 remainingCount 는 [0, LIMIT] 안에 있어야 한다.
        // 예전엔 경계가 20 으로 하드코딩돼 있었는데, 지금 테스트 유저는 리셋으로 구독자(40)가 되므로
        // 정상 응답이 전부 레이스로 오판됐다. 경계를 한도와 맞춘다.
        let remaining;
        try { remaining = res.json('result.remainingCount'); } catch (e) { remaining = undefined; }
        if (remaining !== undefined && remaining !== null && (remaining < 0 || remaining > LIMIT)) {
            raceSuspect.add(1);
            console.warn(`[RACE SUSPECT] VU=${__VU} user=test${userNum}@test.com remaining=${remaining} (한도 ${LIMIT})`);
        }
    });
}

// ─── teardown: 서버측 분위수 + DB 반영 건수 -> Gauge 로 전달 ──────────────────
export function teardown(data) {
    const base = data.base || {};
    const nowBuckets = scrapeBuckets(TARGET_URI);
    const q = (p) => quantileFromBuckets(base.buckets, nowBuckets, p);
    const fmt = (v) => (v === null || v === undefined ? 'n/a' : (v * 1000).toFixed(1) + 'ms');
    const serverP50 = q(0.5), serverP95 = q(0.95), serverP99 = q(0.99);
    const serverTotal = bucketTotal(base.buckets, nowBuckets);

    const now = loadTestState(data.adminToken);
    const deltaSum = (now && base.state) ? (now.reqCountSum - base.state.reqCountSum) : null;

    const toMs = (v) => (v === null || v === undefined ? -1 : Math.round(v * 100000) / 100);
    gServerP50.add(toMs(serverP50));
    gServerP95.add(toMs(serverP95));
    gServerP99.add(toMs(serverP99));
    gServerTotal.add(serverTotal);
    gDbDelta.add(deltaSum === null ? -1 : deltaSum);

    const cfg = base.state;
    gCfgVersion.add(cfg ? cfg.getDummyVersion : -1);
    gCfgInterceptor.add(cfg ? (cfg.interceptorEnabled ? 1 : 0) : -1);
    gCfgVt.add(cfg ? (cfg.virtualThreads ? 1 : 0) : -1);
    gCfgPool.add(cfg ? cfg.hikariPoolSize : -1);

    console.log('\n[teardown] 서버측 p50/p95/p99 = ' + fmt(serverP50) + ' / ' + fmt(serverP95) + ' / ' + fmt(serverP99)
        + '  |  서버 처리 ' + serverTotal + '건  |  Δ reqCount = ' + (deltaSum === null ? 'n/a (관리자 미지정)' : deltaSum)
        + '  → 상세 판정은 아래 요약/결과 파일 참조');
}

// ─── handleSummary: 정합성 3개 불변식 판정 + 파일 저장 ────────────────────────
export function handleSummary(data) {
    const m = data.metrics || {};
    const cnt = (n) => (m[n] && m[n].values && m[n].values.count) || 0;
    const gauge = (n) => {
        const v = m[n] && m[n].values ? m[n].values.value : undefined;
        return (v === undefined || v === -1) ? null : v;
    };

    const success = cnt('success_200_count');
    const reject  = cnt('reject_429_count');
    const limit   = cnt('limit_hit_400_count');
    const other   = cnt('unexpected_status_count');
    const missing = cnt('token_missing_count');
    const dbDelta = gauge('db_req_count_delta');
    const cfgVer  = gauge('cfg_getdummy_version');
    const config = cfgVer === null ? null : {
        getDummyVersion: cfgVer,
        interceptorEnabled: gauge('cfg_interceptor_enabled') === 1,
        virtualThreads: gauge('cfg_virtual_threads') === 1,
        hikariPoolSize: gauge('cfg_hikari_pool_size'),
    };

    const verdict = {
        users: USERS, concurrent: CONCURRENT, total_vu: TOTAL_VU,
        success_200: success, reject_429: reject, edge_429: cnt('edge_429_count'),
        limit_hit_400: limit, unexpected_status: other, token_missing: missing,
        race_condition_suspect: cnt('race_condition_suspect'),
        server_p50_ms: gauge('server_p50_ms'),
        server_p95_ms: gauge('server_p95_ms'),
        server_p99_ms: gauge('server_p99_ms'),
        server_handled_total: gauge('server_handled_total'),
        db_req_count_delta: dbDelta,
        config: config,
    };

    // (1) Lost Update — 같은 유저의 동시 요청이 서로의 reqCount 증가를 덮어썼는가
    let lostLine;
    if (dbDelta === null) {
        lostLine = '  (1) Lost Update  : 판정 불가 (ADMIN_EMAIL/ADMIN_PASSWORD 미지정 또는 Admin API 실패)';
    } else {
        verdict.lost_update_count = Math.max(0, success - dbDelta);
        verdict.lost_update_ok = verdict.lost_update_count === 0;
        lostLine = '  (1) Lost Update  : success_200=' + success + ' vs Δ reqCount=' + dbDelta + ' → '
            + (verdict.lost_update_ok ? '유실 없음 OK' : '[!] ' + verdict.lost_update_count + '건 유실');
        if (m.lost_update_count) {
            m.lost_update_count.values.count = verdict.lost_update_count;
            if (m.lost_update_count.thresholds && m.lost_update_count.thresholds['count<1']) {
                m.lost_update_count.thresholds['count<1'].ok = verdict.lost_update_ok;
            }
        }
    }

    // (3) 응답 분류 누락 검증 — 총 VU 가 성공/거절/한도 중 하나로 전부 분류돼야 한다
    const classified = success + reject + limit;
    verdict.vu_equation_sum = classified;
    verdict.vu_equation_ok = classified === TOTAL_VU;

    const fmt = (v) => (v === null ? 'n/a' : v.toFixed(1) + 'ms');
    const processingReport = [
        '',
        '────────────── 정합성 판정 (유저 ' + USERS + '명 × 동시 ' + CONCURRENT + ' = ' + TOTAL_VU + ' VU) ──────────────',
        '  [서버 설정]      getDummy v' + (config ? config.getDummyVersion : '?')
            + ' · interceptor=' + (config ? config.interceptorEnabled : '?')
            + ' · VT=' + (config ? config.virtualThreads : '?')
            + ' · hikari=' + (config ? config.hikariPoolSize : '?'),
        '  [응답 분포]      200=' + success + ' · 429=' + reject + ' · 400(한도)=' + limit
            + ' · 기타=' + other + ' · 토큰없음=' + missing,
        '  [서버측 지연]    p50 / p95 / p99 = ' + fmt(verdict.server_p50_ms) + ' / ' + fmt(verdict.server_p95_ms)
            + ' / ' + fmt(verdict.server_p99_ms),
        '',
        lostLine,
        '  (2) 레이스 감지  : race_condition_suspect=' + verdict.race_condition_suspect
            + ' (remainingCount 가 [0, ' + LIMIT + '] 을 벗어난 횟수)',
        '  (3) 분류 검증식  : ' + TOTAL_VU + ' VU == 200+429+400 = ' + classified + ' → '
            + (verdict.vu_equation_ok ? 'OK' : '[!] 불일치 — 타임아웃/5xx 로 유실된 응답이 있다'),
        '',
        '  ※ 429 는 정상 동작이다 — V2=분산락 획득 실패(CANT_GET_LOCK), V3·V4=인터셉터 SETNX 거절.',
        '    따닥을 막았다는 증거이며, 그 비용은 reject_429_duration_ms 로 비교한다.',
        '  ※ 이 회차는 성능이 아니라 불변식을 본다. 서버를 포화시키지 않는 VU 로 도는 것이 전제다.',
        '──────────────────────────────────────────────────────────────────────────',
    ].join('\n');

    const tag = __ENV.TAG || __ENV.STAGE || tagFromState(config);
    return summaryFiles(data, {
        load: TOTAL_VU + 'vu',   // closed model -> CLAUDE_INIT 규칙상 vu 표기
        duration: USERS + 'x' + CONCURRENT,
        tag: tag,
        scenario: 'closed model 따닥 스파이크 (per-vu-iterations, 유저 ' + USERS + '명 × 동시 ' + CONCURRENT + ', 1회 발사)',
        params: { users: USERS, concurrent: CONCURRENT, total_vu: TOTAL_VU, daily_limit: LIMIT },
        processingReport: processingReport,
        extra: verdict,
    });
}
