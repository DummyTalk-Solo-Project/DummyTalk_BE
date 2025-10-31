package DummyTalk.DummyTalk_BE;

import DummyTalk.DummyTalk_BE.domain.entity.User;
import DummyTalk.DummyTalk_BE.domain.repository.UserRepository;
import DummyTalk.DummyTalk_BE.domain.service.dummy.impl.DummyServiceImplV3;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.DummyHandler;
import DummyTalk.DummyTalk_BE.global.exception.handler.UserHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class DummyServiceTest {

    @Autowired
    private DummyServiceImplV3 dummyService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserRepository userRepository;


    private final Integer threadCount = 100;
    private static final String TEST_EMAIL = "user1";
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_QUIZ_ID = 1L;
    private static final String QUIZ_HASH_KEY = "quiz";
    private static final String QUIZ_ANSWER_LIST_KEY = "quiz:answer";

    @Test
    @DisplayName("동일 유저 동시성 테스트")
    void solveQuizConcurrencyTest() throws InterruptedException {

        /// given
        final ExecutorService executorService = Executors.newFixedThreadPool(32); // 32개의 멀티 스레드 환경 허용
        final CountDownLatch countDownLatch = new CountDownLatch(threadCount); // 일단 100개 정도

        User user = userRepository.findByEmail(TEST_EMAIL).orElseThrow(() -> new UserHandler(ErrorCode.CANT_FIND_USER));
        // 실패(중복제출 예외) 횟수 카운트
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);


        /// when
        for (int i =0; i< threadCount; i++){
            executorService.submit(() -> {
                try{
                    // 일단 1로만 고정하자, 차피 한 사람이 여러 문제를 푸는 걸 확인하는 게 더 중요
                    dummyService.solveQuiz(TEST_EMAIL, TEST_QUIZ_ID, 1);
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
        boolean finished = countDownLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThat(finished).isTrue(); // 10초 안에 모든 작업이 끝나야 함

        // --- 검증 ---
        // 1. Redis List (순위)에 몇 건이 쌓였는지 확인
        Long answerListSize = redisTemplate.opsForList().size(QUIZ_ANSWER_LIST_KEY);

        // 2. Redis Hash (제출여부)에 해당 유저가 기록되었는지 확인
        Object submitRecord = redisTemplate.opsForHash().get(QUIZ_HASH_KEY, TEST_USER_ID.toString());

        // 3. 콘솔에 결과 출력 (디버깅용)
        System.out.println("--- 테스트 결과 ---");
        System.out.println("총 시도 횟수: " + threadCount);
        System.out.println("성공 횟수 (SuccessCount): " + successCount.get());
        System.out.println("실패 횟수 (FailCount): " + failCount.get());
        System.out.println("Redis List 저장 개수: " + answerListSize);
        System.out.println("Redis Hash 제출 기록: " + submitRecord);

        // [핵심] Race Condition 증명
        // 만약 로직이 완벽했다면, successCount는 1, failCount는 99, answerListSize는 1이 되어야 합니다.
        // 하지만 Race Condition 때문에 여러 스레드가 'check'를 통과합니다.

        // **이 테스트의 통과 조건은 "버그가 발생했는가?"입니다.**
        assertThat(successCount.get()).isGreaterThan(1); // 1번보다 많이 성공 (버그)
        assertThat(answerListSize).isGreaterThan(1L);  // List에도 1개보다 많이 쌓임 (버그)

        // 성공한 횟수와 List에 쌓인 개수는 정확히 일치해야 합니다. (데이터 정합성 문제까지는 없었다면)
        assertThat(successCount.get()).isEqualTo(answerListSize.intValue());

        // Hash에는 어쨌든 값이 기록되어 있어야 합니다.
        assertThat(submitRecord).isNotNull();
    }
}
