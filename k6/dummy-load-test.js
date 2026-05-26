/**
 * DummyTalk 트래픽 테스트 시나리오 (EC2 운영환경 대상)
 *
 * 전제 조건:
 *   1. test1~200@test.com 유저가 EC2 DB에 생성되어 있어야 함 (k6/setup.sql 참고)
 *   2. Redis 에 dummy:{RARITY} Set 이 세팅되어 있어야 함 (앱 기동 시 DummyDataLoader 자동 처리)
 *   3. AccessToken 만료 시간 > 테스트 전체 시간 (RTR 갱신 로직 생략)
 *
 * 실행:
 *   k6 run -e BASE_URL=http://<EC2_IP>:8080 k6/dummy-load-test.js
 *   k6 run -e BASE_URL=http://<EC2_IP>:8080 --out json=k6/results/dummy-result.json k6/dummy-load-test.js
 */

import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ─── 커스텀 메트릭 ─────────────────────────────────────────────────────────────
const dummyTotal     = new Counter('dummy_requests_total');     // 전체 뽑기 횟수
const limitHitRate   = new Rate('dummy_limit_hit_rate');        // 20회 제한 도달 비율
const raceSuspect    = new Counter('race_condition_suspect');   // remainingCount 이상 감지 횟수
const dummyDuration  = new Trend('dummy_req_duration_ms');      // 뽑기 응답 시간

// ─── 시나리오 설정 ──────────────────────────────────────────────────────────────
export const options = {
  scenarios: {
    dummy_users: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },  // 0→50 VU 증가 (동시 접속 램프업)
        { duration: '2m',  target: 50 },  // 50 VU 유지 (동시성 집중 테스트)
        { duration: '30s', target: 0 },   // 종료
      ],
    },
  },
  thresholds: {
    http_req_duration:    ['p(95)<2000'],  // 뽑기 응답 95%가 2초 이내
    http_req_failed:      ['rate<0.05'],   // 예상치 못한 에러율 5% 미만
    dummy_req_duration_ms:['p(95)<1500'],  // 뽑기 자체 응답 1.5초 이내
  },
};

// EC2 대상 실행: k6 run -e BASE_URL=http://<EC2_IP>:8080 k6/dummy-load-test.js
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// ─── 메인 시나리오 ──────────────────────────────────────────────────────────────
export default function () {
  // VU 번호로 테스트 유저 분배 (1~200 순환)
  const userNum = (__VU % 200) + 1;
  const email    = `test${userNum}@test.com`;
  const password = 'Test1234!';

  let accessToken = null;

  // 1. 로그인
  group('1_login', () => {
    const res = http.post(
      `${BASE_URL}/api/members/login`,
      JSON.stringify({ email, password }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    const ok = check(res, {
      'login: status 200': (r) => r.status === 200,
      'login: accessToken 존재': (r) => r.json('result.accessToken') !== null,
    });

    if (ok) {
      accessToken = res.json('result.accessToken');
    }
  });

  if (!accessToken) {
    console.error(`[VU=${__VU}] 로그인 실패, 시나리오 중단`);
    return;
  }

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`,
  };

  // 2. Dummy 뽑기 루프 (FE 라이브타이핑 3초 sleep 포함, 최대 20회)
  group('2_dummy_loop', () => {
    for (let i = 0; i < 20; i++) {
      const start = Date.now();
      const res = http.get(`${BASE_URL}/api/dummies/dummy`, { headers });
      dummyDuration.add(Date.now() - start);
      dummyTotal.add(1);

      const body = res.json();

      // 20회 소진 에러 (정상 케이스)
      if (res.status === 400) {
        const code = body?.code;
        check(res, { 'dummy: 20회 제한 도달 (정상)': () => code === 'DUMMY_4001' });
        limitHitRate.add(1);
        break;
      }

      check(res, { 'dummy: status 200': (r) => r.status === 200 });

      const remaining = body?.result?.remainingCount;

      // Race Condition 감지: remainingCount 가 음수거나 20 초과 시 이상 징후
      if (remaining !== undefined && (remaining < 0 || remaining > 20)) {
        raceSuspect.add(1);
        console.warn(`[RACE SUSPECT] VU=${__VU}, iter=${i}, remaining=${remaining}`);
      }

      if (remaining === 0) {
        limitHitRate.add(1);
        break;
      }

      // FE 라이브타이핑 애니메이션 대기 시간 (3초)
      sleep(3);
    }
  });

  // 3. 마이페이지 확인
  group('3_mypage', () => {
    const res = http.get(`${BASE_URL}/api/members/my-page`, { headers });
    check(res, { 'mypage: status 200': (r) => r.status === 200 });
  });
}