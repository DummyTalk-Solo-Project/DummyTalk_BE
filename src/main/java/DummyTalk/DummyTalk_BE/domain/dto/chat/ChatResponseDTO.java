package DummyTalk.DummyTalk_BE.domain.dto.chat;

import lombok.Builder;

@Builder
public class ChatResponseDTO {

    public static class GetDummyData{
        private String msg;
    }

}
