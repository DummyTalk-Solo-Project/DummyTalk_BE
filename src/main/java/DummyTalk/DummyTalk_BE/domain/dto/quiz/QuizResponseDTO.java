package DummyTalk.DummyTalk_BE.domain.dto.quiz;

import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.global.converter.StringListConverter;
import jakarta.persistence.Convert;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class QuizResponseDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizRedisDTO {

        private QuizStatus status;

        private String title;

        private String description;

        private Integer answer;

        private List<String> answerList;

        private LocalDateTime startTime;

        private LocalDateTime endTime;
    }
}
