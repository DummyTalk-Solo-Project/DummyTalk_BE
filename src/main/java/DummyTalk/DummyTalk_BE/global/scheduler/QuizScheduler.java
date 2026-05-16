package DummyTalk.DummyTalk_BE.global.scheduler;

import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.global.scheduler.task.QuizTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class QuizScheduler {

    private final QuizTask quizTask;
    
    public Runnable controlQuiz(Long quizId, QuizStatus status) {
        return () -> {
            log.info("[QuizScheduler - Execution] Start task for quizId: {}, status: {}", quizId, status);
            try {
                quizTask.updateQuizStatus(quizId, status);
                if (status == QuizStatus.CLOSE) {
                    quizTask.settleQuizReward(quizId); // CLOSE 전환 시 별도 트랜잭션 -> 보상 정산 실행 (CLOSE 후 실행하게끔?)
                }
            } catch (Exception e) {
                log.error("[QuizScheduler - Error] Failed to update quiz status", e);
            }
        };
    }
}
