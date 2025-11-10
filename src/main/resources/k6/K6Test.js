import http from 'k6/http';
import { check } from 'k6';
import { execution } from 'k6/execution'; // VU/iteration 정보를 가져오기 위해 import

// const BASE_URL = 'http://<EC2_퍼블릭_IP>:8080';
const BASE_URL = 'http://localhost:8080';

const TOTAL_USERS = 100; // 총 유저
const REQS_PER_USER = 5; // 유저 당 n번 요청
const CONCURRENT_VUS = 100; // 100개의 스레드로 동시 실행

function shuffle(array) {
    let currentIndex = array.length, randomIndex;
    while (currentIndex != 0) {
        randomIndex = Math.floor(Math.random() * currentIndex);
        currentIndex--;
        [array[currentIndex], array[randomIndex]] = [
            array[randomIndex], array[currentIndex]];
    }
    return array;
}

// 1. [Init Context] 테스트 시작 전에 딱 한 번 실행됩니다.
// 토탈 유저 * 유저 당 n번의 요청 데이터 미리 생성
const allRequests = [];
for (let i = 1; i <= TOTAL_USERS; i++) {
    const email = `user${i}@email.com`;
    for (let j = 0; j < REQS_PER_USER; j++) {
        allRequests.push({
            email: email,
            answer: 1, // answer는 1로 통일
        });
    }
}
shuffle(allRequests);


// 2. [Options] 테스트 옵션 설정
export const options = {
    // 'shared-iterations'는 여러 VU(스레드)가 iterations(총 작업량)을 공유하는 방식입니다.
    // 100개의 스레드가 500개의 작업을 나눠서 처리합니다.
    scenarios: {
        quiz_stress: {
            executor: 'shared-iterations',
            vus: CONCURRENT_VUS,        // 동시 실행 스레드 수 (100)
            iterations: allRequests.length, // 총 실행할 작업 수 (500)
            maxDuration: '10m',         // (안전장치) 10분 이상 걸리면 테스트 중지
        },
    },
};

// 3. [Default Function] 각 VU(스레드)가 반복 실행하는 메인 로직
export default function () {
    // execution.scenario.iterationInTest는 0부터 499까지 증가하는 고유 ID입니다.
    // 섞여있는 요청 데이터에서 이번 스레드가 처리할 데이터를 순서대로 하나 꺼냅니다.
    const requestData = allRequests[execution.scenario.iterationInTest];

    // POST 요청할 URL
    const url = `${BASE_URL}/api/quiz2`;

    // @RequestParam은 'application/x-www-form-urlencoded' 형식을 사용합니다.
    // k6는 JS 객체를 body로 넘기면 자동으로 이 형식으로 변환해줍니다.
    const payload = {
        email: requestData.email,
        answer: requestData.answer,
    };

    // 4. POST 요청 실행
    const res = http.post(url, payload);

    // 5. 응답 확인 (결과에 'checks'로 집계됨)
    check(res, {
        'status is 200': (r) => r.status === 200,
    });
}