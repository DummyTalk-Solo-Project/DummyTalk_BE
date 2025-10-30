import http from 'k6/http';
import { check } from 'k6';
import { SharedArray } from 'k6/data';

// 1. 테스트 데이터 불러오기
// SharedArray를 사용해 모든 가상 유저(VU)가 메모리에서 데이터를 공유합니다.
const data = new SharedArray('quizData', function () {
    // 9999개의 데이터가 담긴 JSON 파일을 엽니다.
    return JSON.parse(open('./data.json'));
});

// 2. 테스트 옵션 설정
export const options = {
    stages: [
        // '거의 동시적으로'를 구현하기 위해 짧은 시간 안에 9999명까지 ramp-up합니다.
        { duration: '10s', target: 9999 }, // 10초 동안 9999명까지 트래픽 증가
        { duration: '30s', target: 9999 }, // 30초 동안 9999명 유지
    ],
};

// 3. 메인 테스트 함수 (각 VU가 실행)
export default function () {
    // __VU는 1부터 시작하는 가상 유저 ID입니다.
    // data 배열은 0부터 시작하므로 인덱스를 맞춥니다 (user1 -> data[0])
    const vuIndex = __VU - 1;

    // 9999명을 초과하는 VU가 실행될 경우를 대비 (데이터가 없으면 테스트 중단)
    if (vuIndex >= data.length) {
        return;
    }

    // 1:1로 매핑된 고유 데이터 가져오기
    const uniqueData = data[vuIndex];

    // POST 요청할 URL (컨트롤러와 일치)
    const url = 'http://localhost:8080/api/quiz';

    // @RequestParam을 사용하는 POST 요청은 'application/x-www-form-urlencoded' 형식입니다.
    // k6는 객체를 body로 전달하면 자동으로 이 형식으로 변환해줍니다.
    // (컨트롤러 메서드의 @RequestParam("id")와 @RequestParam("answer")에 맞게 키 이름을 설정)
    const payload = {
        id: uniqueData.quizId,
        answer: uniqueData.answer,
    };

    // 4. POST 요청 실행
    const res = http.post(url, payload);

    // 5. 응답 확인 (선택 사항이지만 권장)
    check(res, {
        'status is 200': (r) => r.status === 200,
    });
}