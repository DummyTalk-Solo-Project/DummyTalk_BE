package DummyTalk.DummyTalk_BE.domain.dto.dummy;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

public class DummyResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class GetDummyQuizResponseDTO{
        private String title;
        private List<String> answerList = new ArrayList<>();
        private Integer answer;
        private String description;
    }
}
