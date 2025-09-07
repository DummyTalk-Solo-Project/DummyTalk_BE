package DummyTalk.DummyTalk_BE.domain.service.user;

import DummyTalk.DummyTalk_BE.domain.dto.user.UserRequestDTO;

public interface UserService {

    void sendEmail();

    void signIn(UserRequestDTO.SignInRequestDTO request);

    void login();
}
