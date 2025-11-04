import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

// --- 테스트 전 설정 ---
const BASE_URL = 'http://<EC2_퍼블릭_IP>:8080'; // [중요] EC2 서버 주소로 변경
const EMAIL_TO_TEST = 'user1@email.com';     // [중요] 동시성 테스트를 수행할 단일 유저
const QUIZ_ID = 1;
// --------------------

// 커스텀 카운터 (성공/실패 집계)
let successCount = new Counter('quiz_solve_success');
let failAlreadySubmitCount = new Counter('quiz_solve_fail_already_submit');
let otherFailCount = new Counter('quiz_solve_fail_other');

export const options = {
    scenarios: {
        concurrent_users: {
            executor: 'per-vu-iterations', // 각 VU(가상유저)가 iterations 만큼 실행
            vus: 100,         // 100명의 가상 유저
            iterations: 1,    // 1번씩만 실행
            maxDuration: '30s',
        },
    },
};

export default function () {
    // [변경] URL에 쿼리 파라미터로 email 추가
    const url = `${BASE_URL}/api/quiz/${QUIZ_ID}/solve?email=${EMAIL_TO_TEST}`;

    // 요청 본문 (퀴즈 정답)
    const payload = JSON.stringify({
        answer: 1,
    });

    // [변경] 'Authorization' 헤더가 제거된 params
    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // API 호출
    const res = http.post(url, payload, params);

    // 결과 확인
    const isSuccess = check(res, {
        'is status 200 (Success)': (r) => r.status === 200,
    });

    if (isSuccess) {
        successCount.add(1);
        return; // 성공 시 종료
    }

    // 중복 제출 실패(409)인지 확인
    const isAlreadySubmit = check(res, {
        'is status 409 (Already Submit)': (r) => r.status === 409,
    });

    if (isAlreadySubmit) {
        failAlreadySubmitCount.add(1);
    } else {
        // 200, 409 외의 다른 모든 에러
        otherFailCount.add(1);
    }
}