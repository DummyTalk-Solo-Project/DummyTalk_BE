package DummyTalk.DummyTalk_BE.domain.dto.dummy;

import DummyTalk.DummyTalk_BE.domain.entity.Quiz;
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

public class DummyRespDTO {

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

        public static GetQuizInfoResponseDTO createDTO(Quiz quiz){
            return GetQuizInfoResponseDTO.builder()
                    .status(quiz.getStatus()).title(quiz.getTitle()).answerList(quiz.getAnswerList()).build();
        }

    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetDummyRespDTO{
        private Long dummyId;
        private String title;
        private String content;
        private String rarityName;
        private Boolean isPityTriggered;        // 이번 뽑기가 천장 발동으로 획득된 경우 true
        private Boolean isNextPityTriggered;    // 다음 뽑기에서 천장 발동 확정인 경우 true
        private Integer remainingCount; // 오늘 남은 요청 횟수
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

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckQuizDTO {
        int activeCount;
        int poolSize;
    }

}
