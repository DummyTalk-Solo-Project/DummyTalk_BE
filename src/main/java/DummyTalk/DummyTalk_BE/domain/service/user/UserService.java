package DummyTalk.DummyTalk_BE.domain.service.user;

import DummyTalk.DummyTalk_BE.domain.dto.user.UserRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.user.UserResponseDTO;

public interface UserService {

    void sendVerificationEmail(String email);

    void verifyEmail(UserRequestDTO.VerificationRequestDTO requestDTO);

    void signIn(UserRequestDTO.SignInRequestDTO request);

    UserResponseDTO.LoginSuccessDTO login(UserRequestDTO.LoginRequestDTO requestDTO);

    void withdraw (String email);
}
