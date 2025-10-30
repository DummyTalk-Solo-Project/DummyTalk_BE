package DummyTalk.DummyTalk_BE.domain.dto.user;

import lombok.*;

import java.time.LocalDateTime;

public class UserResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginSuccessDTO{
        private String username;
        private String accessToken;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class GetUserResponseDTO {
        private String username;
        private String email;

        // user - info
        private Integer reqCount;
        private Boolean isSubscribe;
        private LocalDateTime subsExprDate;
    }
}
