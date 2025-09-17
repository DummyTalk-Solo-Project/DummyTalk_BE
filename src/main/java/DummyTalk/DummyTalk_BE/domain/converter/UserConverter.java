package DummyTalk.DummyTalk_BE.domain.converter;

import DummyTalk.DummyTalk_BE.domain.dto.user.UserRequestDTO;
import DummyTalk.DummyTalk_BE.domain.entity.constant.Login;
import DummyTalk.DummyTalk_BE.domain.entity.info.Info;
import DummyTalk.DummyTalk_BE.domain.entity.user.User;

public class UserConverter {

    public static User toNewUser (UserRequestDTO.SignInRequestDTO request) {
        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .login(Login.NORMAL)
                .build();
    }

    public static UserRequestDTO.AIRequestDTO toAIRequestDTO (User user, Info info){
        return UserRequestDTO.AIRequestDTO.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .login(user.getLogin())
                .info(info)
                .build();

    }
}
