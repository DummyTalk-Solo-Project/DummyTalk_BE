/**
 * ─────────────────────────────────────────────────────────────────────────────
 * K6 전체 사용자 여정 테스트
 * 50 VU × 3회 반복 (약 1.5초 간격) — 실제 사용자 패턴 재현
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * [시나리오 흐름]
 *   setup()  → 50개 계정 토큰 일괄 발급 (매 iteration마다 로그인 생략)
 *   default  → 가챠 3회 → 퀴즈 1회 → 마이페이지 조회
 *
 * [재현 목표 버그들]
 *
 *   Bug A — 뱃지 중복 부여 (@EventListener + @Async 조합 문제)
 *     - @EventListener는 publishEvent() 즉시(커밋 전) 이벤트 처리 시작 가능
 *     - 두 요청이 동시에 totalDummyCount=10 또는 =100을 읽어
 *       BadgeExecutor 스레드 2개가 동시에 existsByMemberAndBadge()=false → save()
 *     - 버그 징후: 마이페이지 뱃지 목록에 같은 이름의 뱃지가 2개
 *     - DB 검증:
 *       SELECT member_id, badge_id, COUNT(*) as cnt
 *       FROM member_badge
 *       GROUP BY member_id, badge_id
 *       HAVING COUNT(*) > 1;
 *
 *   Bug B — getDummy() reqCount Race Condition
 *     - remainingCount 가 음수거나 정해진 한도(20/40)를 초과하면 이상 징후
 *     - 기존 dummy-load-test.js 의 raceSuspect 메트릭과 동일한 감지 로직 포함
 *
 * [setup() 토큰 공유 전략]
 *   - setup()에서 50개 토큰을 미리 발급 → 배열로 반환
 *   - 각 VU는 data.tokens[__VU - 1] 로 자신의 토큰 접근
 *   - 장점: 매 iteration마다 로그인 없음 → 로그인 부하와 비즈니스 로직 부하 분리
 *   - 장점: 로그인 실패가 테스트를 오염시키지 않음
 *
 * [전제 조건]
 *   1. test1~50@test.com 유저 DB에 생성 (k6/setup.sql 참고)
 *   2. Redis dummy:{RARITY} Set 세팅 완료
 *   3. AccessToken 만료 시간 > 테스트 전체 시간
 *   4. (선택) 퀴즈가 OPEN 상태 — 없으면 퀴즈 단계는 QUIZ_NOT_OPEN 에러로 스킵
 *
 * [실행 방법]
 *   k6 run k6/full-journey-test.js
 *   k6 run --out json=k6/results/journey-result.json k6/full-journey-test.js
 *   k6 run --out influxdb=http://localhost:8086/k6 k6/full-journey-test.js  (Grafana 연동)
 *
 * [결과 확인 포인트]
 *   1. 콘솔에서 [BADGE BUG], [RACE SUSPECT] 로그 개수 확인
 *   2. 커스텀 메트릭 badge_duplicate_suspect, race_condition_suspect 값
 *   3. 테스트 후 DB 쿼리로 실제 중복 확인 (위 SQL 참고)
 *   4. --out json 으로 저장 후 메트릭 분석
 * ─────────────────────────────────────────────────────────────────────────────
 */

import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ─── 커스텀 메트릭 ─────────────────────────────────────────────────────────────
// [핵심] 0보다 크면 버그 발현 가능성
const badgeDuplicateSuspect = new Counter('badge_duplicate_suspect'); // 뱃지 중복 의심 (마이페이지에서 감지)
const raceConditionSuspect  = new Counter('race_condition_suspect');  // remainingCount 이상 (기존 메트릭 유지)

// 흐름 추적
const dummyTotal      = new Counter('dummy_requests_total');
const dummyLimitHit   = new Rate('dummy_limit_hit_rate');
const quizSubmitTotal = new Counter('quiz_submit_total');
const quizSuccess     = new Counter('quiz_submit_success');
const quizFailed      = new Counter('quiz_submit_failed');

// 응답 시간
const dummyDuration = new Trend('dummy_req_duration_ms');
const quizDuration  = new Trend('quiz_req_duration_ms');

const BASE_URL = 'http://localhost:8080';
const VU_COUNT = 50;

// ─── 시나리오 설정 ──────────────────────────────────────────────────────────────
export const options = {
  scenarios: {
    full_journey: {
      executor: 'per-vu-iterations',
      vus: VU_COUNT,
      iterations: 3,       // 각 VU가 3번 반복 (5초 간격이면 총 ~15s)
      maxDuration: '60s',
    },
  },
  thresholds: {
    http_req_duration:          ['p(95)<3000'],
    http_req_failed:            ['rate<0.1'],
    dummy_req_duration_ms:      ['p(95)<2000'],
    // [핵심 검증] 0이어야 정상. 0보다 크면 버그 발현 의심
    'badge_duplicate_suspect':  ['count<1'],
    'race_condition_suspect':   ['count<1'],
  },
};

// ─── setup(): 토큰 일괄 발급 ────────────────────────────────────────────────────
// 한 번만 실행되며 모든 VU가 공유함 (로그인 부하 분리)
export function setup() {
  console.log(`[setup] ${VU_COUNT}개 계정 토큰 발급 시작...`);

  const tokens = [];
  const failed = [];

  for (let i = 1; i <= VU_COUNT; i++) {
    const email = `test${i}@test.com`;
    const res = http.post(
      `${BASE_URL}/api/members/login`,
      JSON.stringify({ email, password: 'Test1234!' }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    const token = res.json('result.accessToken');
    if (token) {
      tokens.push(token);
    } else {
      failed.push(email);
      tokens.push(null); // 인덱스 유지를 위해 null 삽입
    }
  }

  if (failed.length > 0) {
    console.warn(`[setup] 로그인 실패 계정 (${failed.length}개): ${failed.join(', ')}`);
    console.warn('[setup] k6/setup.sql 로 테스트 계정이 생성되어 있는지 확인하세요');
  }

  console.log(`[setup] 토큰 발급 완료: ${tokens.filter(Boolean).length}/${VU_COUNT}`);
  return { tokens };
}

// ─── 메인 시나리오 ──────────────────────────────────────────────────────────────
export default function (data) {
  // VU 번호 기반으로 토큰 할당 (1~50 분배)
  const vuIndex     = (__VU - 1) % VU_COUNT;
  const accessToken = data.tokens[vuIndex];
  const userNum     = vuIndex + 1;

  if (!accessToken) {
    console.error(`[VU=${__VU}] 토큰 없음 (test${userNum}@test.com 로그인 실패), 시나리오 중단`);
    return;
  }

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`,
  };

  // ── 1. Dummy 뽑기 3회 ─────────────────────────────────────────────────────
  group('1_gacha_3times', () => {
    let prevRemaining = null;

    for (let i = 0; i < 3; i++) {
      const start = Date.now();
      const res   = http.get(`${BASE_URL}/api/dummies/dummy`, { headers });
      dummyDuration.add(Date.now() - start);
      dummyTotal.add(1);

      const body = res.json();

      // 20회 제한 도달 (정상 종료)
      if (res.status === 400 && body?.code?.includes('DUMMY')) {
        dummyLimitHit.add(1);
        console.log(`[VU=${__VU}] 가챠 한도 도달, 루프 탈출`);
        break;
      }

      check(res, { '가챠: 200 OK': (r) => r.status === 200 });

      const remaining = body?.result?.remainingCount;

      // Race Condition 감지: remainingCount 음수 또는 한도 초과
      if (remaining !== undefined && (remaining < 0 || remaining > 40)) {
        raceConditionSuspect.add(1);
        console.warn(
          `[RACE SUSPECT] VU=${__VU}, iter=${i}, remaining=${remaining}` +
          ` — 정상 범위(0~40) 이탈`
        );
      }

      // 연속 요청에서 remainingCount 증가 감지 (count가 역전되면 이상)
      if (prevRemaining !== null && remaining !== null && remaining > prevRemaining) {
        raceConditionSuspect.add(1);
        console.warn(
          `[RACE SUSPECT] remainingCount 역전! VU=${__VU}, prev=${prevRemaining} → curr=${remaining}`
        );
      }
      prevRemaining = remaining;

      // 사용자의 FE 라이브타이핑 애니메이션 대기 시뮬레이션 (1.5초)
      sleep(1.5);
    }
  });

  // ── 2. 퀴즈 제출 (OPEN 상태일 때만 유효) ───────────────────────────────────
  group('2_quiz_solve', () => {
    // 현재 활성 퀴즈 조회
    const quizRes = http.get(`${BASE_URL}/api/dummies/quiz`, { headers });

    if (quizRes.status !== 200) {
      // QUIZ_NOT_OPEN 등 — 퀴즈가 없으면 조용히 스킵
      check(quizRes, {
        '퀴즈: 없음 또는 미오픈 (스킵)': (r) => r.status === 400 || r.status === 404,
      });
      return;
    }

    const quizId = quizRes.json('result.quizId');
    if (!quizId) return;

    const start = Date.now();
    const solveRes = http.post(
      `${BASE_URL}/api/dummies/quiz?id=${quizId}&answer=1`,
      null,
      { headers }
    );
    quizDuration.add(Date.now() - start);
    quizSubmitTotal.add(1);

    if (solveRes.status === 200) {
      quizSuccess.add(1);
      check(solveRes, { '퀴즈: 제출 성공': () => true });
    } else {
      quizFailed.add(1);
      const code = solveRes.json('code') || '';
      // ALREADY_SUBMIT, WRONG_ANSWER, TICKET_IS_DONE 은 정상 에러
      check(solveRes, {
        '퀴즈: 정상 에러 응답': (r) =>
          r.body.includes('ALREADY_SUBMIT') ||
          r.body.includes('TICKET_IS_DONE') ||
          r.body.includes('WRONG_ANSWER') ||
          r.body.includes('QUIZ_IS_CLOSED'),
      });
    }
  });

  // ── 3. 마이페이지 조회 + 뱃지 중복 감지 ────────────────────────────────────
  group('3_mypage_badge_check', () => {
    const res = http.get(`${BASE_URL}/api/members/my-page`, { headers });
    check(res, { '마이페이지: 200 OK': (r) => r.status === 200 });

    if (res.status !== 200) return;

    const badges = res.json('result.badges') || [];

    // 뱃지 이름 중복 감지 (같은 이름의 뱃지가 2개이상이면 버그)
    const badgeNames = badges.map(b => b.name);
    const badgeSet   = new Set(badgeNames);

    if (badgeNames.length !== badgeSet.size) {
      badgeDuplicateSuspect.add(1);
      const duplicates = badgeNames.filter((name, idx) => badgeNames.indexOf(name) !== idx);
      console.error(
        `[BADGE BUG] ★ 중복 뱃지 감지! VU=${__VU} (test${userNum}@test.com)` +
        `\n  → 전체 뱃지: ${JSON.stringify(badgeNames)}` +
        `\n  → 중복 항목: ${JSON.stringify(duplicates)}` +
        `\n  → DB 검증: SELECT member_id, badge_id, COUNT(*) FROM member_badge GROUP BY member_id, badge_id HAVING COUNT(*)>1;`
      );
    }

    check(res, {
      '뱃지: 중복 없음': () => badgeNames.length === badgeSet.size,
    });
  });

  // 다음 iteration 전 대기 (5초 간격 시뮬레이션)
  sleep(2);
}

// ─── 테스트 종료 후 요약 ────────────────────────────────────────────────────────
export function handleSummary(data) {
  const badgeBugCnt = data.metrics['badge_duplicate_suspect']?.values?.count || 0;
  const raceBugCnt  = data.metrics['race_condition_suspect']?.values?.count  || 0;
  const totalDummy  = data.metrics['dummy_requests_total']?.values?.count    || 0;
  const quizOk      = data.metrics['quiz_submit_success']?.values?.count     || 0;
  const quizAll     = data.metrics['quiz_submit_total']?.values?.count       || 0;
  const p95Dummy    = data.metrics['dummy_req_duration_ms']?.values?.['p(95)'] || 0;

  console.log('\n═══════════════════════════════════════════════════════');
  console.log('  전체 사용자 여정 테스트 결과 요약');
  console.log('═══════════════════════════════════════════════════════');
  console.log(`  [badge_duplicate_suspect] : ${badgeBugCnt}  ← 0이어야 정상, 0보다 크면 뱃지 중복 버그`);
  console.log(`  [race_condition_suspect]  : ${raceBugCnt}   ← 0이어야 정상, 0보다 크면 reqCount 이상`);
  console.log('───────────────────────────────────────────────────────');
  console.log(`  총 가챠 요청: ${totalDummy}`);
  console.log(`  퀴즈 제출: ${quizOk}/${quizAll} 성공`);
  console.log(`  가챠 p95 응답시간: ${p95Dummy.toFixed(0)}ms`);
  console.log('───────────────────────────────────────────────────────');

  if (badgeBugCnt > 0) {
    console.log('\n  ★ 뱃지 중복 버그 발현! 아래 쿼리로 확인:');
    console.log('  SELECT member_id, badge_id, COUNT(*) as cnt');
    console.log('  FROM member_badge');
    console.log('  GROUP BY member_id, badge_id');
    console.log('  HAVING COUNT(*) > 1;');
  }

  if (raceBugCnt > 0) {
    console.log('\n  ★ reqCount Race Condition 의심! 로그에서 [RACE SUSPECT] 검색하세요.');
    console.log('  remainingCount 가 음수거나 40 초과인 응답이 있습니다.');
  }

  if (badgeBugCnt === 0 && raceBugCnt === 0) {
    console.log('\n  ✓ 감지된 버그 없음');
    console.log('  → 버그 재현이 안 됐을 수 있음: 동시 요청 타이밍, DB 응답 속도에 따라 재현 확률 달라짐');
    console.log('  → quiz-concurrent-test.js (Phase 2) 로 더 극단적인 동시 제출 테스트 권장');
  }

  console.log('═══════════════════════════════════════════════════════\n');

  return {
    stdout: JSON.stringify(data, null, 2),
  };
}
