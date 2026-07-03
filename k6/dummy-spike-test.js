/**
 * getDummy() 동시성 제어 단계별 스파이크 테스트
 *
 * 목적:
 *   동시성 개선 3단계를 같은 시나리오로 비교 → 결과 그래프 나란히 비교
 *   Stage 1 (현재 코드)  → race_condition_suspect 발생, remainingCount 음수 감지
 *   Stage 2 (비관적 락)  → 정합성 보장, but hikaricp_connections_pending 급증 예상
 *   Stage 3 (Redisson)  → fast_fail_429_count 발생, DB 커넥션 풀 안정 확인
 *
 * 시나리오 설계:
 *   - USERS명의 유저가 각각 CONCURRENT개의 따닥 요청을 동시에 발사 후 종료 (1회)
 *   - ramping-vus(램프업) 없이 per-vu-iterations(1회성 스파이크)로 즉각 동시성 유발
 *   - 로그인은 setup()에서 선처리 → 본 테스트에서 로그인 오버헤드 제거, 뽑기 요청에 집중
 *   - sleep 없음 → 같은 유저에 배정된 CONCURRENT개의 VU가 거의 동시에 요청 발사
 *
 * 실행 예시:
 *   [Stage 1 - 현재 코드 (레이스 컨디션 확인)]
 *   k6 run -e BASE_URL=http://localhost:8080 -e USERS=20 -e CONCURRENT=5 k6/dummy-spike-test.js --out json=k6/results/stage1-raw.json
 *
 *   [Stage 2 - 비관적 락 (커넥션 풀 압박 확인)]
 *   k6 run -e BASE_URL=http://localhost:8080 -e USERS=20 -e CONCURRENT=5 k6/dummy-spike-test.js --out json=k6/results/stage2-pessimistic.json
 *
 *   [Stage 3 - Redisson tryLock(0) (빠른 거절 + 풀 안정 확인)]
 *   k6 run -e BASE_URL=http://localhost:8080 -e USERS=20 -e CONCURRENT=5 k6/dummy-spike-test.js --out json=k6/results/stage3-redisson.json
 *
 * 환경변수:
 *   BASE_URL    : 대상 서버 (기본: http://localhost:8080)
 *   USERS       : 유저 수 (기본: 10 — T3.Small 기준, 총 VU = 10×5 = 50)
 *   CONCURRENT  : 유저당 동시 따닥 요청 수 (기본: 5) → 총 VU = USERS × CONCURRENT
 *
 * ── T3.Small VU 가이드 ────────────────────────────────────────────────────────
 *   기본 (따닥 재현):  USERS=10, CONCURRENT=5  → 총 50 VU (T3.Small 안정 범위)
 *   강한 스파이크:    USERS=20, CONCURRENT=5  → 총 100 VU (Thread Pool 압박)
 *   극단 스파이크:    USERS=30, CONCURRENT=5  → 총 150 VU (RejectedExecution 재현용)
 *   ※ CONCURRENT 값은 1명당 따닥 VU 수 — 동시성 테스트 핵심이므로 5 유지 권장
 */

import http from 'k6/http';
import { check, group } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// ─── 커스텀 메트릭 ────────────────────────────────────────────────────────────
const dummyDuration      = new Trend('dummy_req_duration_ms');        // 뽑기 응답 시간 분포
const raceSuspect        = new Counter('race_condition_suspect');     // remainingCount 음수/이상 감지 → Stage 1에서 발생 예상
const fastFailCount      = new Counter('fast_fail_429_count');        // 인터셉터/분산락 즉시 거절 횟수 → Stage 3 전용 지표
const fastFailRate       = new Rate('fast_fail_429_rate');            // 전체 중 429 차단 비율 (인터셉터 효과 측정)
const limitHitCount      = new Counter('limit_hit_count');            // 20회 정상 소진 횟수
const successCount       = new Counter('success_200_count');          // 200 성공 수 → 검증식 VU = success + fast_fail + limit_hit
const rejectDuration     = new Trend('reject_429_duration_ms');       // 429 거절 경로만의 응답 시간 = "거절 비용" (Stage2 락 vs Stage3 인터셉터 비교 지표)

// ─── 파라미터 ─────────────────────────────────────────────────────────────────
const BASE_URL   = __ENV.BASE_URL              || 'http://localhost:8080';
const USERS      = parseInt(__ENV.USERS)       || 10;  // T3.Small 기본값 (총 VU = 10×5 = 50)
const CONCURRENT = parseInt(__ENV.CONCURRENT)  || 5;   // 유저당 따닥 VU 수 (따닥 재현 핵심, 변경 비권장)

// ─── 시나리오 설정 ────────────────────────────────────────────────────────────
export const options = {
  // p99까지 K6 터미널 요약에 표시 (Grafana 없이도 레이턴시 분포 확인 가능)
  summaryTrendStats: ['avg', 'p(50)', 'p(90)', 'p(95)', 'p(99)', 'max'],

  // setup() 로그인 루프는 순차 실행 — USERS=400(VU 2000)이면 원격 기준 수 분 소요 (기본 60s로는 부족)
  setupTimeout: '10m',

  scenarios: {
    ddotg_spike: {
      // per-vu-iterations: 각 VU가 정확히 N번 실행 후 종료 (ramping-vus와 달리 반복 없음)
      // → 전체 1회 스파이크만 발생, 유지 구간 없음
      executor: 'per-vu-iterations',
      vus: USERS * CONCURRENT,
      iterations: 1,
      // VU 2000 스파이크 시 Tomcat 200 스레드 큐 대기가 길어짐 → 여유 확보
      maxDuration: '5m',
    },
  },

  thresholds: {
    // 429는 responseCallback으로 expectedStatuses 처리 → http_req_failed 에서 제외됨
    http_req_failed:        ['rate<0.1'],
    // T3.Small 스파이크 기준 — 순간 폭증이므로 일반 부하보다 여유 설정
    dummy_req_duration_ms:  ['p(95)<5000'],
    // [핵심] Stage 1에서 이 threshold 실패 예상, Stage 3에서 0이어야 개선 증거
    race_condition_suspect: ['count<1'],
    // Stage 3에서 fast_fail이 전체의 (CONCURRENT-1)/CONCURRENT 비율로 발생하는 것이 정상
    // ex) CONCURRENT=5 → 같은 유저 5개 VU 중 1개만 통과, 4개는 차단 → rate ≈ 0.8
  },
};

// ─── 선행 로그인 (setup은 단일 스레드, VU 시작 전 1회만 실행) ─────────────────
export function setup() {
  console.log(`[setup] ${USERS}명 로그인 시작, 총 VU=${USERS * CONCURRENT} (USERS=${USERS}, CONCURRENT=${CONCURRENT})`);
  const tokenMap = {};

  // memberId 1은 관리자 계정 → test2@test.com 부터 시작 (i=2 ~ USERS+1)
  for (let i = 2; i <= USERS + 1; i++) {
    const res = http.post(
      `${BASE_URL}/api/members/login`,
      JSON.stringify({ email: `test${i}@test.com`, password: 'Test1234!' }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    const token = res.json('result.accessToken');
    if (!token) {
      console.error(`[setup] test${i}@test.com 로그인 실패 (status=${res.status})`);
    }
    tokenMap[String(i)] = token || '';
  }

  console.log(`[setup] 로그인 완료 → 스파이크 발사 준비`);
  return tokenMap;
}

// ─── 메인 시나리오: 따닥 1회 발사 ────────────────────────────────────────────
export default function (tokenMap) {
  // VU → 유저 매핑: CONCURRENT개의 VU가 동일 유저에 배정 → 따닥 시뮬레이션
  // ex) CONCURRENT=10: VU 1~10 → user2(test2@test.com), VU 11~20 → user3, ...
  // memberId 1은 관리자 계정이므로 +2부터 시작
  const userNum = Math.floor((__VU - 1) / CONCURRENT) + 2;
  const token   = tokenMap[String(userNum)];

  if (!token) {
    console.error(`[VU=${__VU}] user${userNum} 토큰 없음, 스킵`);
    return;
  }

  const headers = {
    'Content-Type':  'application/json',
    'Authorization': `Bearer ${token}`,
  };

  group('getDummy_ddotg', () => {
    const start = Date.now();
    const res   = http.get(`${BASE_URL}/api/dummies/dummy`, {
      headers,
      // 429 = 인터셉터(Stage3) 또는 분산락(Stage2) 따닥 차단 → 정상 동작, http_req_failed 제외
      responseCallback: http.expectedStatuses(200, 400, 429),
      // 고VU 시 Tomcat 큐 대기가 K6 기본 60s를 초과하면 측정 데이터가 통째로 유실됨 → 상향
      timeout: '120s',
    });
    const elapsed = Date.now() - start;
    dummyDuration.add(elapsed);

    // [Stage 2/3] 따닥 즉시 차단
    // Stage 2: @DistributedLock(waitTime=0) → CANT_GET_LOCK → GeneralException → 429 또는 500
    // Stage 3: IdempotentRequestInterceptor → DUPLICATE_REQUEST → 429
    if (res.status === 429) {
      fastFailCount.add(1);
      fastFailRate.add(1);
      rejectDuration.add(elapsed); // 거절 비용: 인터셉터(DB 진입 전) vs 분산락(AOP 진입 후) 차이 측정
      check(res, { '[Stage2/3] 따닥 즉시 차단 (429 정상)': () => true });
      console.log(`[FAST FAIL] VU=${__VU}, user=test${userNum}@test.com → 인터셉터/분산락 차단`);
      return;
    }
    fastFailRate.add(0); // 정상 통과는 rate에 0 기여

    // 20회 정상 소진
    if (res.status === 400) {
      const code = res.json('code') ?? res.json('result.code');
      if (code === 'DUMMY_4001') {
        limitHitCount.add(1);
        check(res, { '20회 제한 도달 (정상)': () => true });
        return;
      }
    }

    check(res, { 'getDummy 200': (r) => r.status === 200 });
    if (res.status === 200) {
      successCount.add(1);
    }

    // 레이스 컨디션 감지: remainingCount가 음수 or 20 초과 → Stage 1에서 발생 예상
    const remaining = res.json('result.remainingCount');
    if (remaining !== undefined && (remaining < 0 || remaining > 20)) {
      raceSuspect.add(1);
      console.warn(`[RACE SUSPECT] VU=${__VU}, user=test${userNum}@test.com, remaining=${remaining}`);
    }
  });
}

// ─── 결과 자동 수집: 회차별 압축 JSON 저장 ───────────────────────────────────
// run-stage-matrix.ps1이 이 JSON들을 모아 마크다운 표를 자동 생성
// 파일명: k6/results/<STAGE>-vu<총VU>-<USERS>x<CONCURRENT>.json (재실행 시 같은 조합은 덮어씀)
export function handleSummary(data) {
  const m = data.metrics;
  const v = (name, stat) => {
    if (!m[name] || m[name].values[stat] === undefined) return 0;
    return Math.round(m[name].values[stat] * 100) / 100;
  };

  const stage   = __ENV.STAGE || 'stageX';
  const totalVu = USERS * CONCURRENT;
  const success = v('success_200_count', 'count');
  const ff      = v('fast_fail_429_count', 'count');
  const limit   = v('limit_hit_count', 'count');

  const summary = {
    stage:        stage,
    users:        USERS,
    concurrent:   CONCURRENT,
    total_vu:     totalVu,
    avg:          v('dummy_req_duration_ms', 'avg'),
    p50:          v('dummy_req_duration_ms', 'p(50)'),
    p90:          v('dummy_req_duration_ms', 'p(90)'),
    p95:          v('dummy_req_duration_ms', 'p(95)'),
    p99:          v('dummy_req_duration_ms', 'p(99)'),
    max:          v('dummy_req_duration_ms', 'max'),
    // 거절 비용: 429 응답만의 레이턴시 — Stage3 인터셉터가 Stage2 락보다 싸다는 주장의 근거
    reject_p95:   v('reject_429_duration_ms', 'p(95)'),
    reject_p99:   v('reject_429_duration_ms', 'p(99)'),
    success_200:  success,
    fast_fail_429: ff,
    fast_fail_rate: v('fast_fail_429_rate', 'rate'),
    limit_hit:    limit,
    race_suspect: v('race_condition_suspect', 'count'),
    http_req_failed_rate: v('http_req_failed', 'rate'),
    http_reqs_per_sec:    v('http_reqs', 'rate'),
    // 검증식: 모든 VU가 성공/거절/한도소진 중 하나로 분류되어야 함 (불일치 = 유실 or 예외 응답)
    vu_equation_ok: (success + ff + limit) === totalVu,
    vu_equation_sum: success + ff + limit,
    ran_at: new Date().toISOString(),
  };

  const file = `k6/results/${stage}-vu${totalVu}-${USERS}x${CONCURRENT}.json`;
  return {
    [file]:   JSON.stringify(summary, null, 2),
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
  };
}
