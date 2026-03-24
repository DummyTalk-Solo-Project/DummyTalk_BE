package DummyTalk.DummyTalk_BE.domain.dto.dummy;

import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.domain.entity.constant.RarityType;
import lombok.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DummyResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class GetQuizFromAIResponseDTO {
        private String title;
        private List<String> answerList = new ArrayList<>();
        private Integer answer;
        private String description;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetQuizInfoResponseDTO {
        private QuizStatus status; // NOT_OPEN, OPEN, CLOSE
        private Integer userGrade;

        private Long quizId;
        private String title;
        private List<String> answerList = new ArrayList<>();
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetDummyRespDTO{
        private Long dummyId;
        private String title;
        private String content;
        private String rarityName;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetMyDummyDTO {
        public Long dummyId;
        public String title;
        public String content;
        private RarityType name;
        private LocalDateTime createdAt;

        public Long rarityId; // COMMON, RARE, EPIC, SPECIAL
        private String colorCode; // HEX Code
//        private Double probability; // 55% 30% 12% 3%
    }

}
