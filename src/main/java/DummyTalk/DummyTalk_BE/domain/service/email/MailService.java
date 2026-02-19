package DummyTalk.DummyTalk_BE.domain.service.email;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendReminderEmail(String toEmail, String nickname) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[DummyTalk] " + nickname + "님, 보고 싶어요!");
        message.setText("안녕하세요 " + nickname + "님!\n\n" +
                "DummyTalk을 떠나신 지 5일이 지났네요.\n" +
                "새로운 대화들이 당신을 기다리고 있습니다. 지금 바로 접속해 보세요!");

        mailSender.send(message);
    }
}