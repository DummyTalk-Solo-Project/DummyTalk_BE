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
        return () -> { // 여기서는 로그만
            log.info("[QuizScheduler - Execution] Start task for quizId: {}, status: {}", quizId, status);
            try {
                // 실제 DB 수정을 수행하는 트랜잭션 메서드 호출
                quizTask.updateQuizStatus(quizId, status);
            } catch (Exception e) {
                log.error("[QuizScheduler - Error] Failed to update quiz status", e);
            }
        };
    }
}
