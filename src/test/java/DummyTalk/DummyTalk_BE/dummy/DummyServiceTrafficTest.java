package DummyTalk.DummyTalk_BE.dummy;

import DummyTalk.DummyTalk_BE.domain.dto.user.UserRequestDTO;
import DummyTalk.DummyTalk_BE.domain.entity.User;
import DummyTalk.DummyTalk_BE.domain.repository.UserRepository;
import DummyTalk.DummyTalk_BE.domain.service.dummy.impl.DummyServiceImplV3;
import DummyTalk.DummyTalk_BE.domain.service.user.impl.UserServiceImpl;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.DummyHandler;
import DummyTalk.DummyTalk_BE.global.exception.handler.UserHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StopWatch;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@SpringBootTest
public class DummyServiceTrafficTest {

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private DummyServiceImplV3 dummyService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    @Value("${spring.data.redis.host}")
    private String host;


    private final Integer threadCount = 500;
    private static final String TEST_EMAIL = "jijysun@naver.com";
    private static final String QUIZ_HASH_KEY = "quiz";
    private static final String QUIZ_ANSWER_LIST_KEY = "quiz:answer";


    // @BeforeEach
    void createTestUser(){
        for (int i = 1; i< 101; i++){
            String username = "user" + i;
            String email = username + "@email.com";

            userService.signIn(UserRequestDTO.SignInRequestDTO.builder()
                    .email(email)
                    .username(username)
                    .password("1234") // 비밀번호는 공통
                    .build());;
        }
    }

    @Test
    @DisplayName("동일 유저 동시성 테스트 (Local)")
    void solveQuizConcurrencyTest() throws InterruptedException {

        /// given
        final ExecutorService executorService = Executors.newFixedThreadPool(32); // 32개의 멀티 스레드 환경 허용
        final CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        User user = userRepository.findByEmail(TEST_EMAIL).orElseThrow(() -> new UserHandler(ErrorCode.CANT_FIND_USER));
        // 실패(중복제출 예외) 횟수 카운트
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        redisTemplate.delete(QUIZ_HASH_KEY);
        redisTemplate.delete(QUIZ_ANSWER_LIST_KEY);
        dummyService.openQuiz("jijysun@naver.com", LocalDateTime.of(2025, 11, 1, 13, 13));


        // [추가] 성능 측정을 위한 StopWatch 생성
        StopWatch stopWatch = new StopWatch();

        /// when
        log.info("--- 스레드 동시성 테스트 시작 ---");
        stopWatch.start(); // [추가] 시간 측정 시작

        for (int i =0; i< threadCount; i++){ // 사용자가 한 번에 엄청난 문제 풀이 요청
            executorService.submit(() -> {
                try{
                    // 일단 1로만 고정하자, 차피 한 사람이 여러 문제를 푸는 걸 확인하는 게 더 중요
//                    dummyService.solveQuiz(TEST_EMAIL, TEST_QUIZ_ID,1);
                    dummyService.solveQuizVer2(TEST_EMAIL, 1);
                    successCount.incrementAndGet();
                }
                catch (DummyHandler e){ // 이미 푼 경우에 대해서만 failCounting
                    if (e.getErrorCode() == ErrorCode.ALREADY_SUBMIT) {
                        failCount.incrementAndGet();
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                finally {
                    countDownLatch.countDown();
                }
            });
        }


        /// then

        // 모든 스레드가 끝날 때까지 10초간 대기
        countDownLatch.await(20, TimeUnit.SECONDS);
        stopWatch.stop(); // [추가] 시간 측정 종료
        executorService.shutdown();

//        assertThat(finished).isTrue(); // 10초 안에 모든 작업이 끝나야 함

        // 1. Redis List (순위)에 몇 건이 쌓였는지 확인
        Long answerListSize = redisTemplate.opsForList().size(QUIZ_ANSWER_LIST_KEY);

        // 2. Redis Hash (제출여부)에 해당 유저가 기록되었는지 확인
        Object submitRecord = redisTemplate.opsForHash().get(QUIZ_HASH_KEY, user.getId().toString());

        // 3. 콘솔에 결과 출력 (디버깅용)
        log.info("==========테스트 결과==========");
        System.out.println("총 시도 횟수: " + threadCount);
        System.out.println("성공 횟수 (SuccessCount): " + successCount.get());
        System.out.println("실패 횟수 (FailCount): " + failCount.get());
        System.out.println("Redis List 저장 개수: " + answerListSize);
        System.out.println("Redis Hash 제출 기록: " + submitRecord);
        // [추가] 성능 측정 결과 출력
        log.info("--- 성능 측정 결과 (StopWatch) ---");
        double totalTimeSeconds = stopWatch.getTotalTimeSeconds();
        System.out.println("총 실행 시간 (ms): " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("초당 처리량 (RPS): " + String.format("%.2f", (threadCount / totalTimeSeconds)) + " req/s");


        // 테스트 통과 조건 == "버그가 발생했는가?"
        assertThat(successCount.get()).isEqualTo(1); // 1번보다 많이 성공 (버그)
        assertThat(answerListSize).isEqualTo(1L);  // List에도 1개보다 많이 쌓임 (버그)

        // 성공한 횟수와 List에 쌓인 개수는 정확히 일치해야 합니다. (데이터 정합성 문제까지는 없었다면)
        assertThat(successCount.get()).isEqualTo(answerListSize.intValue());

        // Hash에는 어쨌든 값이 기록되어 있어야 합니다.
        assertThat(submitRecord).isNotNull();
    }



    @Test
    @DisplayName("동일 유저 동시성 테스트 (WebClient API 호출)")
    void solveQuizApiConcurrencyTest() {

        /// given
        final ExecutorService executorService = Executors.newFixedThreadPool(32);
        final CountDownLatch countDownLatch = new CountDownLatch(threadCount); // 100

        // [추가] API 호출을 위한 WebClient
        final WebClient webClient = WebClient.builder()
                .baseUrl("http://"+host+":8080") // [중요] 테스트 대상 서버 주소
                .build();

        // [추가] API Request Body
        final Map<String, Integer> requestBody = Map.of("answer", 1);

        // Redis 초기화
        redisTemplate.delete(QUIZ_HASH_KEY);
        redisTemplate.delete(QUIZ_ANSWER_LIST_KEY);

        // 퀴즈 오픈 (편의상 서비스 직접 호출)
        try {
            dummyService.openQuiz("jijysun@naver.com", LocalDateTime.now().minusHours(1));
        } catch (Exception e) {
            log.warn("퀴즈 설정 중 오류 (이미 열려있을 수 있음): {}", e.getMessage());
        }

        // [수정] user 객체는 테스트 마지막 검증(then)에서 ID를 사용할 때 필요
        User user = userRepository.findByEmail(TEST_EMAIL)
                .orElseThrow(() -> new UserHandler(ErrorCode.CANT_FIND_USER));

        // 실패(중복제출 예외) 횟수 카운트
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);


        /// when
        for (int i = 0; i < threadCount; i++) { // 사용자가 한 번에 엄청난 문제 풀이 요청
            executorService.submit(() -> {
                try {
                    // [수정] 서비스 직접 호출 -> WebClient API 호출
                    webClient.post()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/api/quiz/")
                                    .queryParam("email", TEST_EMAIL)
                                    .queryParam("answer", "1")// [중요] email 파라미터 추가
                                    .build())
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(requestBody)
                            .retrieve()
                            .toBodilessEntity() // 응답 본문은 무시
                            .block(); // [중요] 동기식 대기

                    successCount.incrementAndGet();

                } catch (WebClientResponseException e) { // [수중] API 예외 처리
                    // 이미 푼 경우 (409 Conflict라고 가정)
                    if (e.getStatusCode() == HttpStatus.CONFLICT) {
                        failCount.incrementAndGet();
                    } else {
                        log.error("API 호출 중 예상치 못한 에러: {}", e.getStatusCode(), e);
                    }
                } catch (Exception e) {
                    log.error("WebClient 실행 중 예외: {}", e.getMessage(), e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }


        /// then
//        boolean finished = countDownLatch.await(10, TimeUnit.SECONDS); // 10초로 복원
        executorService.shutdown();

//        assertThat(finished).isTrue(); // 10초 안에 모든 작업이 끝나야 함

        // API 호출이 모두 끝난 뒤, Redis DB 상태를 직접 검증
        Long answerListSize = redisTemplate.opsForList().size(QUIZ_ANSWER_LIST_KEY);
        Object submitRecord = redisTemplate.opsForHash().get(QUIZ_HASH_KEY, user.getId().toString());

        // 3. 콘솔에 결과 출력 (디버깅용)
        log.info("--- API 동시성 테스트 결과 ---");
        System.out.println("총 시도 횟수: " + threadCount);
        System.out.println("성공 횟수 (SuccessCount): " + successCount.get());
        System.out.println("실패 횟수 (FailCount): " + failCount.get());
        System.out.println("Redis List 저장 개수: " + answerListSize);
        System.out.println("Redis Hash 제출 기록: " + submitRecord);


        // [중요] 테스트 검증: '버그가 발생했는가?'
        // 버그가 발생했다면(예: 19번 성공) 이 테스트는 '통과(PASS)'해야 합니다.
        // 따라서 'isEqualTo(1)'이 아닌 'isGreaterThan(1)'이 맞습니다.

        // (주석 처리된 코드가 '버그 수정 후' 검증용 코드입니다)
        // assertThat(successCount.get()).isEqualTo(1);
        // assertThat(answerListSize).isEqualTo(1L);

        // [수정] '버그 재현'을 검증하는 어설션
        assertThat(successCount.get())
                .withFailMessage("Race Condition이 발생하지 않았습니다. (성공 횟수: %s)", successCount.get())
                .isGreaterThan(1);
        assertThat(answerListSize)
                .withFailMessage("Race Condition이 발생하지 않았습니다. (List 크기: %s)", answerListSize)
                .isGreaterThan(1L);

        // 성공한 횟수와 List에 쌓인 개수는 정확히 일치해야 합니다. (데이터 정합성)
        assertThat(successCount.get()).isEqualTo(answerListSize.intValue());

        // Hash에는 어쨌든 값이 기록되어 있어야 합니다.
        assertThat(submitRecord).isNotNull();
    }
}
