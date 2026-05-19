package DummyTalk.DummyTalk_BE.global.scheduler.task;

import DummyTalk.DummyTalk_BE.domain.entity.Quiz;
import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.InfoRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.QuizRepository;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.QuizHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuizTask {

    private final QuizRepository quizRepository;
    private final InfoRepository infoRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public void updateQuizStatus(Long quizId, QuizStatus status) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizHandler(ErrorCode.WRONG_QUIZ));
        quiz.changeStatus(status);
        log.info("[QuizTask - updateQuizStatus()] - 퀴즈 {} 상태 변경: {}", quizId, status);
    }

    @Transactional
    public void settleQuizReward(Long quizId) { // 기존 트랜잭션에서 분리
        List<Object> topEntries = redisTemplate.opsForList().range("quiz:" + quizId, 0, 4);

        if (topEntries == null || topEntries.isEmpty()) {
            log.info("[QuizTask - settleQuizReward()] - 퀴즈 {} 정답자 없음, 정산 스킵", quizId);
            return;
        }

        List<Long> beneficiaries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Object entry : topEntries) {
            Long memberId = Long.parseLong(((String) entry).split(":")[0]);

            infoRepository.findByMember_Id(memberId).ifPresentOrElse(info -> {
                // 기존 구독자 = 만료일 기준 +7일, or 지금부터 +7일 (스택)
                LocalDateTime base = (info.getSubsExprDate() != null && info.getSubsExprDate().isAfter(now))
                        ? info.getSubsExprDate()
                        : now;
                info.updateSubsExprDate(true, base.plusDays(7));
                beneficiaries.add(memberId);
            }, () -> log.warn("[QuizTask - settleQuizReward()] - Info 없음 memberId={}", memberId));
        }

        // 정산 완료 표시 — 스케줄러 재실행 시 중복 지급 방지
        redisTemplate.delete("quiz:" + quizId);

        log.info("[QuizTask - settleQuizReward()] - 퀴즈 {} 정산 완료 | 수혜자: {}", quizId, beneficiaries);
    }
}
