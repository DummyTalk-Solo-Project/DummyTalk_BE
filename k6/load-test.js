/**
 * DummyTalk 부하 테스트
 *
 * 시나리오: 로그인 → 가챠 20번(일일 한도) → 21번째 한도 초과 확인
 *
 * [단일 실행 - 1명 1회]
 *   k6 run -e BASE_URL=http://<EC2_IP>  EMAIL=test@example.com PASSWORD=Test1234! load-test.js
 *
 * [N명으로 확장 - 미리 생성된 계정 N개 필요]
 *   k6 run \
 *     -e BASE_URL=http://<EC2_IP> \
 *     -e EMAIL=test@example.com \
 *     -e PASSWORD=Test1234! \
 *     -e VUS=5 -e ITERATIONS=5 \
 *     k6/load-test.js
 *
 * ※ N명 확장 시 이메일 형식: test+1@example.com, test+2@example.com ...
 *    Gmail은 +suffix 수신을 지원하므로 한 계정으로 여러 테스트 계정 생성 가능
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

// ── 환경변수 ────────────────────────────────────────────────────
const BASE_URL   = __ENV.BASE_URL   || 'http://localhost:8080';
const EMAIL      = __ENV.EMAIL      || 'test@example.com';
const PASSWORD   = __ENV.PASSWORD   || 'Test1234!';
const VUS        = parseInt(__ENV.VUS)        || 1;
const ITERATIONS = parseInt(__ENV.ITERATIONS) || 1;

// ── 옵션 ────────────────────────────────────────────────────────
export const options = {
  vus:        VUS,
  iterations: ITERATIONS,
  thresholds: {
    http_req_duration:      ['p(95)<3000'], // 전체 95%ile < 3s
    'gacha_success_count':  ['count>=1'],   // 가챠 최소 1회 이상 성공
    'gacha_limit_hit_rate': ['rate>=0.99'], // 21번째 한도 거부 99% 이상
  },
};

// ── 커스텀 메트릭 ────────────────────────────────────────────────
const gachaSuccessCount = new Counter('gacha_success_count');
const gachaLimitHitRate = new Rate('gacha_limit_hit_rate');

// ── 헬퍼 ────────────────────────────────────────────────────────
function jsonHeaders(token) {
  const h = { 'Content-Type': 'application/json' };
  if (token) h['Authorization'] = `Bearer ${token}`;
  return h;
}

// N명 확장 시 VU별 고유 이메일 생성
// VUS=1 → test@example.com 그대로 사용
// VUS>1 → test+1@example.com, test+2@example.com ...
function resolveEmail() {
  if (VUS <= 1) return EMAIL;
  const [local, domain] = EMAIL.split('@');
  return `${local}+${__VU}@${domain}`;
}

// ── 메인 시나리오 ────────────────────────────────────────────────
export default function () {
  const email = resolveEmail();

  // ── 1단계: 로그인 ────────────────────────────────────────────
  const loginRes = http.post(
    `${BASE_URL}/api/members/login`,
    JSON.stringify({ email, password: PASSWORD }),
    { headers: jsonHeaders() },
  );
  check(loginRes, { '[1] 로그인 200': (r) => r.status === 200 });

  // 서버가 Authorization 응답 헤더로 토큰 반환
  const raw   = loginRes.headers['Authorization'] || loginRes.headers['authorization'] || '';
  const token = raw.replace('Bearer ', '').trim();
  if (!token) {
    console.error(`[VU ${__VU}] 토큰 없음 — 로그인 실패로 중단`);
    return;
  }

  const headers = jsonHeaders(token);

  // ── 2단계: 가챠 20번 (일일 한도) ─────────────────────────────
  let successCount = 0;
  for (let i = 1; i <= 20; i++) {
    const res = http.get(`${BASE_URL}/api/dummies/dummy`, { headers });
    const ok  = check(res, { '[2] 가챠 200': (r) => r.status === 200 });
    if (ok) {
      successCount++;
      gachaSuccessCount.add(1);
    }
    sleep(0.3);
  }
  console.log(`[VU ${__VU}] 가챠 성공: ${successCount}/20`);

  // ── 3단계: 21번째 — 한도 초과 거부 확인 ─────────────────────
  const overRes    = http.get(`${BASE_URL}/api/dummies/dummy`, { headers });
  const isRejected = check(overRes, {
    '[3] 21번째 한도 초과 4xx': (r) => r.status >= 400 && r.status < 500,
  });
  gachaLimitHitRate.add(isRejected);
  console.log(`[VU ${__VU}] 21번째 응답: ${overRes.status} (${isRejected ? '정상 거부' : '예상 외 응답'})`);
}
