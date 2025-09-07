package DummyTalk.DummyTalk_BE.domain.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestDTO {

    public static class SignInRequestDTO{
        public String email;
        public String password;
    }
}
