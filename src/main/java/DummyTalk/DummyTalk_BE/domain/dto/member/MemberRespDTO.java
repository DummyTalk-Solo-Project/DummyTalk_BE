package DummyTalk.DummyTalk_BE.domain.dto.member;

import DummyTalk.DummyTalk_BE.global.security.jwt.JWT;
import lombok.*;

import java.time.LocalDateTime;

public class MemberRespDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginSuccessDTO{
        private Boolean isSuccess;
        private String memberName;
        private String accessToken;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfoDTO {
        private JWT jwt;
        private String username;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class GetMemberResponseDTO {
        private String memberName;
        private String email;

        // member - info
        private Integer reqCount;
        private Boolean isSubscribe;
        private LocalDateTime subsExprDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FindEmailRespDTO{
        private String email;
    }
}
