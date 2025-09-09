package DummyTalk.DummyTalk_BE.global.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class JWT {
    private String grantType;
    private String accessToken;
    private String refreshToken;
}
