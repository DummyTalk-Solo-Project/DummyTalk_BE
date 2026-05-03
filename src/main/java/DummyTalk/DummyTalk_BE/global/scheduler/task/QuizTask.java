package DummyTalk.DummyTalk_BE.global.scheduler.task;

import DummyTalk.DummyTalk_BE.domain.entity.Quiz;
import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.QuizRepository;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.QuizHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuizTask {

    private final QuizRepository quizRepository;

    @Transactional // 실제 실행 시점에서 트랜잭션
    public void updateQuizStatus(Long quizId, QuizStatus status) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizHandler(ErrorCode.WRONG_QUIZ));
        quiz.changeStatus(status);
        log.info("[QuizTask - Success] Quiz {} status changed to {}", quizId, status);
    }
}
