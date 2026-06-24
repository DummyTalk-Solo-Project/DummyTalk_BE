/**
 * ─────────────────────────────────────────────────────────────────────────────
 * K6 퀴즈 동시성 테스트 — solveQuiz() 중복 제출 버그 재현
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * [재현 목표 버그]
 *   existsByMemberIdAndQuizId() 중복 체크가 PESSIMISTIC_WRITE 락 "밖"에 있어,
 *   같은 사용자의 동시 요청 2개가 둘 다 exists()=false 를 읽고 통과하면:
 *     → 동일 (member_id, quiz_id) 행이 MemberQuiz 테이블에 2개 저장될 수 있음
 *     → Redis List에 같은 memberId가 2번 등록 → 상위 5명 산정 오류
 *   MemberQuiz 테이블에 UNIQUE 제약이 없으므로 DB 레벨 방어도 없음.
 *
 * [시나리오 구성]
 *   Phase 1 — ticket 소진 경쟁 (다른 사용자 50명 동시 제출)
 *     - 50 VU 각자 다른 계정으로 동일 quizId에 동시 제출
 *     - ticket=10 이므로 10명만 성공, 40명은 TICKET_IS_DONE 에러가 정상
 *     - 성공 수가 10을 초과하면 ticket 감소 로직 동시성 버그 의심
 *
 *   Phase 2 — 동일 사용자 동시 2회 제출 (핵심 버그 재현)
 *     - http.batch() 로 동일 계정에서 2개 요청을 최대한 동시에 발사
 *     - 둘 다 200 응답을 받으면 → RACE CONDITION 발현!
 *     - 정상: 하나만 200, 나머지는 ALREADY_SUBMIT (중복 제출 차단)
 *
 * [버그 증거 남기는 방법]
 *   1. 이 테스트 실행 후 아래 SQL 로 DB 직접 확인:
 *      ─── 중복 MemberQuiz 확인 ───────────────────────────────────────────
 *      SELECT member_id, quiz_id, COUNT(*) as cnt
 *      FROM member_quiz
 *      GROUP BY member_id, quiz_id
 *      HAVING COUNT(*) > 1;
 *      → 1행 이상 나오면 버그 발현 증거
 *      ─────────────────────────────────────────────────────────────────────
 *      SELECT COUNT(*) as total_redis_entries   -- Redis List 크기 확인용 참고
 *      FROM member_quiz
 *      WHERE quiz_id = <QUIZ_ID>;
 *      ─────────────────────────────────────────────────────────────────────
 *
 *   2. 테스트 결과 JSON 저장:
 *      k6 run --out json=k6/results/quiz-result.json k6/quiz-concurrent-test.js
 *      → 커스텀 메트릭 'race_condition_detected' 값이 0보다 크면 버그 발현
 *
 *   3. 콘솔 출력에서 [RACE BUG] 또는 [TICKET OVERFLOW] 로그 확인
 *
 * [전제 조건]
 *   1. test1~50@test.com 유저 DB에 생성되어 있어야 함 (k6/setup.sql 참고)
 *   2. OPEN 상태인 퀴즈가 존재해야 함 (환경변수 QUIZ_ID로 지정)
 *      → Admin API로 퀴즈 오픈 후 반환된 quiz.id 값 사용
 *   3. AccessToken 만료 시간 > 테스트 시간 (RTR 갱신 로직 생략)
 *
 * [실행 방법] (EC2 운영환경 대상)
 *   k6 run -e BASE_URL=http://<EC2_IP>:8080 -e QUIZ_ID=<id> k6/quiz-concurrent-test.js
 *   k6 run -e BASE_URL=http://<EC2_IP>:8080 -e QUIZ_ID=1 --out json=k6/results/quiz-result.json k6/quiz-concurrent-test.js
 * ─────────────────────────────────────────────────────────────────────────────
 */

import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ─── 커스텀 메트릭 ─────────────────────────────────────────────────────────────
// [핵심] 이 값이 0보다 크면 버그 발현 증거
const raceConditionDetected   = new Counter('race_condition_detected');   // 동시 2회 제출이 둘 다 성공한 횟수
const ticketOverflow           = new Counter('ticket_overflow');           // ticket=10인데 11번째 이후가 성공한 횟수

// 정상 케이스 카운터
const submitSuccess            = new Counter('submit_success');            // 정상 제출 성공
const submitBlocked            = new Counter('submit_blocked');            // 중복 제출 차단됨 (ALREADY_SUBMIT)
const ticketDone               = new Counter('ticket_done');               // 티켓 소진 (TICKET_IS_DONE)

// 응답 시간 추적
const solveDuration            = new Trend('solve_quiz_duration_ms');

// EC2 대상 실행: k6 run -e BASE_URL=http://<EC2_IP>:8080 -e QUIZ_ID=<id> k6/quiz-concurrent-test.js
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const QUIZ_ID  = __ENV.QUIZ_ID || '1'; // -e QUIZ_ID=<id> 로 지정
const TICKET_LIMIT = 10; // Quiz 생성 시 ticket=10

// T3.Small VU 가이드:
//   Phase 1 (ticket 경쟁): 20 VU → ticket 10개 중 누가 먼저 차지하는지 관찰 충분
//   Phase 2 (따닥 버그):   10 VU → 동일 유저 batch() 2회 발사 × 10명, 버그 재현 충분
//   50 VU / 20 VU (기존) → T3.Small에서 login 오버헤드 + EC2 CPU burst credit 소진 위험

// ─── 시나리오 설정 ──────────────────────────────────────────────────────────────
export const options = {
  scenarios: {

    // Phase 1: 다른 사용자 20명이 동일 퀴즈에 동시 제출 (ticket 소진 경쟁)
    // T3.Small: 50→20 VU. ticket=10이면 20명으로 경쟁 재현 충분
    phase1_ticket_race: {
      executor: 'shared-iterations',
      vus: 20,
      iterations: 20,
      maxDuration: '30s',
      exec: 'phase1',
      tags: { phase: 'ticket_race' },
    },

    // Phase 2: 동일 사용자 동시 2회 제출 (핵심 버그 재현)
    // T3.Small: 20→10 VU. 버그 재현에 10명으로 충분, login 오버헤드 절반 절감
    phase2_duplicate_submit: {
      executor: 'per-vu-iterations',
      vus: 10,
      iterations: 1,
      maxDuration: '30s',
      startTime: '35s',  // Phase 1 완료 후 시작 (퀴즈 재오픈 필요할 수 있음)
      exec: 'phase2',
      tags: { phase: 'duplicate_submit' },
    },
  },

  thresholds: {
    http_req_failed:    ['rate<0.1'],
    // [핵심 검증] race_condition_detected 가 0 이어야 정상. 버그 수정 전엔 0이 아닐 수 있음.
    'race_condition_detected': ['count<1'],
    'ticket_overflow':         ['count<1'],
  },
};

// ─── Phase 1: ticket 소진 경쟁 ─────────────────────────────────────────────────
export function phase1() {
  const userNum = (__VU % 20) + 1;  // T3.Small: 20 VU 기준 순환
  const email   = `test${userNum}@test.com`;

  // 로그인
  const loginRes = http.post(
    `${BASE_URL}/api/members/login`,
    JSON.stringify({ email, password: 'Test1234!' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const accessToken = loginRes.json('result.accessToken');
  if (!accessToken) {
    console.error(`[Phase1] 로그인 실패 - VU=${__VU}, email=${email}`);
    return;
  }

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`,
  };

  group('phase1_solve', () => {
    const start = Date.now();
    const res = http.post(
      `${BASE_URL}/api/dummies/quiz?id=${QUIZ_ID}&answer=1`,
      null,
      { headers }
    );
    solveDuration.add(Date.now() - start);

    const status = res.status;
    const code   = res.json('code') || '';

    if (status === 200) {
      submitSuccess.add(1);

      // ticket=10 초과 성공은 동시성 버그 의심
      if (submitSuccess.count > TICKET_LIMIT) {
        ticketOverflow.add(1);
        console.warn(
          `[TICKET OVERFLOW] ticket 한도(${TICKET_LIMIT}) 초과 성공! ` +
          `VU=${__VU}, 누적 성공수=${submitSuccess.count}, status=${status}`
        );
      }

      check(res, { '[P1] 제출 성공 (200)': () => true });
    } else if (code === 'QUIZ4003' || res.body.includes('TICKET_IS_DONE')) {
      ticketDone.add(1);
      check(res, { '[P1] 티켓 소진 (정상 에러)': () => true });
    } else if (code === 'QUIZ4002' || res.body.includes('ALREADY_SUBMIT')) {
      submitBlocked.add(1);
      check(res, { '[P1] 중복 제출 차단 (정상)': () => true });
    } else {
      console.warn(`[P1 UNEXPECTED] VU=${__VU}, status=${status}, body=${res.body.substring(0, 200)}`);
    }
  });
}

// ─── Phase 2: 동일 사용자 동시 2회 제출 (핵심 버그 재현) ──────────────────────
export function phase2() {
  const userNum = (__VU % 10) + 1;  // T3.Small: 10 VU 기준 순환
  const email   = `test${userNum}@test.com`;

  // 로그인
  const loginRes = http.post(
    `${BASE_URL}/api/members/login`,
    JSON.stringify({ email, password: 'Test1234!' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const accessToken = loginRes.json('result.accessToken');
  if (!accessToken) {
    console.error(`[Phase2] 로그인 실패 - VU=${__VU}`);
    return;
  }

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`,
  };

  group('phase2_double_submit', () => {
    // ★ http.batch() 로 동일 계정에서 2개 요청을 최대한 동시에 발사
    // 두 요청이 서버에 거의 동시 도착 → Race Condition 유발
    const responses = http.batch([
      ['POST', `${BASE_URL}/api/dummies/quiz?id=${QUIZ_ID}&answer=1`, null, { headers, tags: { req: 'first' } }],
      ['POST', `${BASE_URL}/api/dummies/quiz?id=${QUIZ_ID}&answer=1`, null, { headers, tags: { req: 'second' } }],
    ]);

    const res1 = responses[0];
    const res2 = responses[1];

    const ok1 = res1.status === 200;
    const ok2 = res2.status === 200;

    // [버그 감지] 두 요청이 모두 성공하면 Race Condition 발현!
    if (ok1 && ok2) {
      raceConditionDetected.add(1);
      console.error(
        `[RACE BUG] ★★★ 동시 2회 제출이 모두 성공! ★★★ ` +
        `VU=${__VU} (${email}), quizId=${QUIZ_ID}` +
        `\n  → 1번 요청: status=${res1.status}, body=${res1.body.substring(0, 100)}` +
        `\n  → 2번 요청: status=${res2.status}, body=${res2.body.substring(0, 100)}` +
        `\n  → DB 검증: SELECT member_id, quiz_id, COUNT(*) FROM member_quiz GROUP BY member_id, quiz_id HAVING COUNT(*)>1;`
      );
    } else if (ok1 || ok2) {
      // 정상 동작: 하나만 성공, 하나는 차단
      submitSuccess.add(1);
      submitBlocked.add(1);
      const blockedBody = ok1 ? res2.body : res1.body;
      console.log(
        `[Phase2 OK] VU=${__VU} — 하나만 성공, 나머지 차단. 차단 응답: ${blockedBody.substring(0, 100)}`
      );
    } else {
      // 둘 다 실패 (티켓 소진 등)
      console.log(`[Phase2 INFO] VU=${__VU} — 둘 다 실패. res1=${res1.status}, res2=${res2.status}`);
    }

    check(res1, {
      '[P2] 첫 번째 요청 처리됨 (200 or 4xx)': (r) => r.status === 200 || (r.status >= 400 && r.status < 500),
    });
    check(res2, {
      '[P2] 두 번째 요청이 중복 차단되어야 함': (r) => r.status !== 200 || !ok1,
    });
  });
}

// ─── 테스트 종료 후 요약 ────────────────────────────────────────────────────────
export function handleSummary(data) {
  const raceCount   = data.metrics['race_condition_detected']?.values?.count || 0;
  const overflowCnt = data.metrics['ticket_overflow']?.values?.count || 0;

  console.log('\n═══════════════════════════════════════════════════════');
  console.log('  퀴즈 동시성 테스트 결과 요약');
  console.log('═══════════════════════════════════════════════════════');
  console.log(`  [race_condition_detected] : ${raceCount}  ← 0이어야 정상, 0보다 크면 버그 발현`);
  console.log(`  [ticket_overflow]         : ${overflowCnt}  ← 0이어야 정상`);
  console.log(`  [submit_success]          : ${data.metrics['submit_success']?.values?.count || 0}`);
  console.log(`  [submit_blocked]          : ${data.metrics['submit_blocked']?.values?.count || 0}`);
  console.log(`  [ticket_done]             : ${data.metrics['ticket_done']?.values?.count || 0}`);
  console.log('───────────────────────────────────────────────────────');

  if (raceCount > 0 || overflowCnt > 0) {
    console.log('  ★ 버그 발현! DB에서 아래 쿼리로 중복 행 확인하세요:');
    console.log(`  SELECT member_id, quiz_id, COUNT(*) as cnt`);
    console.log(`  FROM member_quiz`);
    console.log(`  GROUP BY member_id, quiz_id`);
    console.log(`  HAVING COUNT(*) > 1;`);
  } else {
    console.log('  ✓ Race Condition 미감지 (수정 완료 또는 재현 조건 미충족)');
    console.log('  → 퀴즈가 이미 CLOSE 상태이거나 ticket이 소진된 경우 Phase 2가 의미 없을 수 있음');
    console.log('  → OPEN 상태 퀴즈로 다시 시도하거나 Phase 2만 단독 실행하여 재확인 필요');
  }
  console.log('═══════════════════════════════════════════════════════\n');

  return {
    stdout: JSON.stringify(data, null, 2),
  };
}
