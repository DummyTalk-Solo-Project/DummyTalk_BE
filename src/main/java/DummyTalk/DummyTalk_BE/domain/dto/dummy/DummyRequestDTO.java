package DummyTalk.DummyTalk_BE.domain.dto.dummy;

import DummyTalk.DummyTalk_BE.domain.entity.constant.TriggerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
public class DummyRequestDTO {

    public static class GetDummyData{
        private String msg;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestInfoDTO {
        private TriggerType triggerType;
        private RequestInfo requestInfo;
    }

    static class RequestInfo {
        private String requestURL;
        private String requestTimeStamp;
        private Boolean isF12;
    }

}
