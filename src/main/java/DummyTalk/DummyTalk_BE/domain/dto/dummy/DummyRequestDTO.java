package DummyTalk.DummyTalk_BE.domain.dto.dummy;

import lombok.Builder;

@Builder
public class DummyRequestDTO {

    public static class GetDummyData{
        private String msg;
    }

}
