package DummyTalk.DummyTalk_BE.domain.dto.user;

import DummyTalk.DummyTalk_BE.global.security.jwt.JwtToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginSuccessDTO{
        private String username;
        private JwtToken jwt;
    }
}
