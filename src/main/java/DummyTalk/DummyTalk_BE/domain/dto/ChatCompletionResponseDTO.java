package DummyTalk.DummyTalk_BE.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionResponseDTO {
    private List<Choice> choices;

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private Message message;
        private String finish_reason;
        private Integer index;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}