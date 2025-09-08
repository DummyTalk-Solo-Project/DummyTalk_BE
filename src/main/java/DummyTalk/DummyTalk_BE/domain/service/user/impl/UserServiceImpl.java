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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 4;

    @Override
    public void sendVerificationEmail(String email) {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper;
        String code;
        try {
            code = generateVerificationCode();
            helper = new MimeMessageHelper(msg, true, "utf-8");
            helper.setTo(email);
            helper.setSubject("더미톡 인증 이메일 알림.");
            helper.setText("<!DOCTYPE html>\n" +
                    "<html lang=\"ko\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "    <style>\n" +
                    "        body {\n" +
                    "            font-family: Arial, sans-serif;\n" +
                    "            background-color: #f4f4f4;\n" +
                    "            margin: 0;\n" +
                    "            padding: 0;\n" +
                    "        }\n" +
                    "        .container {\n" +
                    "            width: 100%;\n" +
                    "            max-width: 600px;\n" +
                    "            margin: 0 auto;\n" +
                    "            background-color: #ffffff;\n" +
                    "            padding: 20px;\n" +
                    "            border-radius: 8px;\n" +
                    "            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);\n" +
                    "        }\n" +
                    "        .header {\n" +
                    "            text-align: center;\n" +
                    "            padding-bottom: 20px;\n" +
                    "            border-bottom: 1px solid #eeeeee;\n" +
                    "        }\n" +
                    "        .content {\n" +
                    "            padding: 20px 0;\n" +
                    "            text-align: center;\n" +
                    "        }\n" +
                    "        .verification-code {\n" +
                    "            display: inline-block;\n" +
                    "            background-color: #e9ecef;\n" +
                    "            padding: 10px 20px;\n" +
                    "            font-size: 24px;\n" +
                    "            font-weight: bold;\n" +
                    "            letter-spacing: 2px;\n" +
                    "            border-radius: 5px;\n" +
                    "            color: #333333;\n" +
                    "            margin: 20px 0;\n" +
                    "        }\n" +
                    "        .link-text {\n" +
                    "            color: #007bff;\n" +
                    "            text-decoration: none;\n" +
                    "            font-weight: bold;\n" +
                    "        }\n" +
                    "        .footer {\n" +
                    "            text-align: center;\n" +
                    "            padding-top: 20px;\n" +
                    "            border-top: 1px solid #eeeeee;\n" +
                    "            font-size: 12px;\n" +
                    "            color: #999999;\n" +
                    "        }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<div class=\"container\">\n" +
                    "    <div class=\"header\">\n" +
                    "        <h2>더미톡 이메일 인증 안내</h2>\n" +
                    "    </div>\n" +
                    "    <div class=\"content\">\n" +
                    "        <p>안녕하세요, 더미톡입니다. 아래 인증 코드를 입력하여 이메일 주소를 인증해 주세요.</p>\n" +
                    "        <span class=\"verification-code\"> "+ code + " </span>\n" +
                    "    </div>\n" +
                    "    <div class=\"footer\">\n" +
                    "        <p>이 메일은 발신 전용입니다. 문의사항은 더미톡 고객센터를 이용해 주세요.</p>\n" +
                    "    </div>\n" +
                    "</div>\n" +
                    "</body>\n" +
                    "</html>", true);
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

    public static String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            // CHARACTERS 문자열의 길이 내에서 무작위 인덱스를 선택
            int randomIndex = random.nextInt(CHARACTERS.length());

            // 해당 인덱스의 문자를 가져와 StringBuilder에 추가
            code.append(CHARACTERS.charAt(randomIndex));
        }

        return code.toString();
    }
}
