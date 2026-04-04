package DummyTalk.DummyTalk_BE.global.scheduler;

import DummyTalk.DummyTalk_BE.domain.entity.Quiz;
import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.QuizRepository;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.QuizHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component
@RequiredArgsConstructor
public class QuizScheduler {

    private final QuizRepository quizRepository;
    
    public Runnable controlQuiz(Long quizId, QuizStatus status) {
        return () -> { // 여기서는 로그만
            log.info("[QuizScheduler - Execution] Start task for quizId: {}, status: {}", quizId, status);
            try {
                // 실제 DB 수정을 수행하는 트랜잭션 메서드 호출
                updateQuizStatus(quizId, status);
            } catch (Exception e) {
                log.error("[QuizScheduler - Error] Failed to update quiz status", e);
            }
        };
    }

    @Transactional // 실제 실행 시점에서 트랜잭션
    public void updateQuizStatus(Long quizId, QuizStatus status) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizHandler(ErrorCode.WRONG_QUIZ));
        quiz.changeStatus(status);
        log.info("[QuizScheduler - Success] Quiz {} status changed to {}", quizId, status);
    }
}
