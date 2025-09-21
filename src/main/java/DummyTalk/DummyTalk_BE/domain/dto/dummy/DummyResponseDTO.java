package DummyTalk.DummyTalk_BE.domain.dto.dummy;

import lombok.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

public class DummyResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class GetQuizResponseDTO {
        private String title;
        private List<String> answerList = new ArrayList<>();
        private Integer answer;
        private String description;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetLastQuizInfoResponseDTO {

    }

}
