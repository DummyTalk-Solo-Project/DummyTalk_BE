package DummyTalk.DummyTalk_BE.domain.service.user.impl;

import DummyTalk.DummyTalk_BE.domain.converter.UserConverter;
import DummyTalk.DummyTalk_BE.domain.dto.user.UserRequestDTO;
import DummyTalk.DummyTalk_BE.domain.entity.user.User;
import DummyTalk.DummyTalk_BE.domain.repository.UserRepository;
import DummyTalk.DummyTalk_BE.domain.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public void sendEmail() {

    }

    @Override
    public void signIn(UserRequestDTO.SignInRequestDTO request) {
        User user = UserConverter.toNewUser(request);
        userRepository.save(user);
    }


    @Override
    public void login() {

        return;
    }
}
