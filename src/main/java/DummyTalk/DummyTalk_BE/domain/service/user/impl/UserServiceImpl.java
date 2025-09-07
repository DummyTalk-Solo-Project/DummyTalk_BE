package DummyTalk.DummyTalk_BE.domain.service.user.impl;

import DummyTalk.DummyTalk_BE.domain.converter.UserConverter;
import DummyTalk.DummyTalk_BE.domain.dto.user.UserRequestDTO;
import DummyTalk.DummyTalk_BE.domain.entity.user.User;
import DummyTalk.DummyTalk_BE.domain.repository.UserRepository;
import DummyTalk.DummyTalk_BE.domain.service.user.UserService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Override
    public void sendVerificationEmail(String email) {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper;
        try {
            helper= new MimeMessageHelper(msg, true, "utf-8");
            helper.setTo(email);
            helper.setSubject("더미톡 인증 이메일 알림.");
            helper.setText("12345");
            helper.setReplyTo("no-reply@mail.com");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

        try{
            mailSender.send(msg);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
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
