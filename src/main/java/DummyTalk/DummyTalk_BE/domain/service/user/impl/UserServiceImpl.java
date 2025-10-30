package DummyTalk.DummyTalk_BE.domain.service.user.impl;

import DummyTalk.DummyTalk_BE.domain.converter.EmailConverter;
import DummyTalk.DummyTalk_BE.domain.converter.UserConverter;
import DummyTalk.DummyTalk_BE.domain.dto.user.UserRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.user.UserResponseDTO;
import DummyTalk.DummyTalk_BE.domain.entity.*;
import DummyTalk.DummyTalk_BE.domain.repository.EmailRepository;
import DummyTalk.DummyTalk_BE.domain.repository.InfoRepository;
import DummyTalk.DummyTalk_BE.domain.repository.UserQuizRepository;
import DummyTalk.DummyTalk_BE.domain.repository.UserRepository;
import DummyTalk.DummyTalk_BE.domain.service.user.UserService;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.UserHandler;
import DummyTalk.DummyTalk_BE.global.security.jwt.JWTProvider;
import DummyTalk.DummyTalk_BE.global.security.jwt.JwtToken;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final JWTProvider jwtProvider;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 4;
    private final EmailRepository emailRepository;
    private final InfoRepository infoRepository;
    private final UserQuizRepository userQuizRepository;


    @Override
    public void sendVerificationEmail(String email) {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper;
        String code;
        LocalDateTime expireTime;
        try {
            code = generateVerificationCode();
            expireTime = LocalDateTime.now();
            helper = new MimeMessageHelper(msg, true, "utf-8");
            helper.setTo(email);
            helper.setSubject("더미톡 인증 이메일 알림.");
            helper.setText("<!DOCTYPE html>\n" +
                    "<html lang=\"ko\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "    <style>\n" +
                    "        @import url('https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;700&display=swap');\n" +
                    "        \n" +
                    "        body {\n" +
                    "            font-family: 'Noto Sans KR', 'Malgun Gothic', 'Apple SD Gothic Neo', Arial, sans-serif;\n" +
                    "            background-color: #f4f6f9;\n" +
                    "            margin: 0;\n" +
                    "            padding: 0;\n" +
                    "        }\n" +
                    "        .container {\n" +
                    "            width: 100%;\n" +
                    "            max-width: 580px;\n" +
                    "            margin: 40px auto;\n" +
                    "            background-color: #ffffff;\n" +
                    "            border-radius: 12px;\n" +
                    "            box-shadow: 0 6px 15px rgba(0, 0, 0, 0.05);\n" +
                    "            overflow: hidden;\n" +
                    "            border: 1px solid #e9ecef;\n" +
                    "        }\n" +
                    "        .header {\n" +
                    "            background-color: #ffffff;\n" +
                    "            padding: 30px 20px 20px;\n" +
                    "            text-align: center;\n" +
                    "        }\n" +
                    "        .header h2 {\n" +
                    "            font-size: 24px;\n" +
                    "            color: #212529;\n" +
                    "            font-weight: 700;\n" +
                    "            margin: 0;\n" +
                    "        }\n" +
                    "        .content {\n" +
                    "            padding: 20px;\n" +
                    "            text-align: center;\n" +
                    "            line-height: 1.6;\n" +
                    "            color: #495057;\n" +
                    "        }\n" +
                    "        .verification-code-container {\n" +
                    "            margin: 30px 0;\n" +
                    "            text-align: center;\n" +
                    "        }\n" +
                    "        .verification-code {\n" +
                    "            display: inline-block;\n" +
                    "            background-color: #eaf3ff;\n" +
                    "            padding: 15px 30px;\n" +
                    "            font-size: 28px;\n" +
                    "            font-weight: bold;\n" +
                    "            letter-spacing: 3px;\n" +
                    "            border-radius: 8px;\n" +
                    "            color: #0056b3;\n" +
                    "            border: 1px dashed #ced4da;\n" +
                    "            -webkit-user-select: all;\n" +
                    "            -moz-user-select: all;\n" +
                    "            -ms-user-select: all;\n" +
                    "            user-select: all;\n" +
                    "        }\n" +
                    "        .instruction {\n" +
                    "            margin-top: 10px;\n" +
                    "            font-size: 14px;\n" +
                    "            color: #6c757d;\n" +
                    "        }\n" +
                    "        .footer {\n" +
                    "            text-align: center;\n" +
                    "            padding: 20px;\n" +
                    "            border-top: 1px solid #e9ecef;\n" +
                    "            font-size: 12px;\n" +
                    "            color: #adb5bd;\n" +
                    "            background-color: #f8f9fa;\n" +
                    "        }\n" +
                    "        .logo {\n" +
                    "            width: 50px;\n" +
                    "            height: 50px;\n" +
                    "            background-color: #007bff;\n" +
                    "            border-radius: 50%;\n" +
                    "            margin: 0 auto 15px;\n" +
                    "        }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<div class=\"container\">\n" +
                    "    <div class=\"header\">\n" +
                    "        \n" +
                    "        <div class=\"logo\"></div>\n" +
                    "        <h2>더미톡 이메일 인증</h2>\n" +
                    "    </div>\n" +
                    "    <div class=\"content\">\n" +
                    "        <p>안녕하세요, 더미톡입니다. 이메일 주소 인증을 위해 아래 코드를 사용해 주세요.</p>\n" +
                    "        <div class=\"verification-code-container\">\n" +
                    "            <span class=\"verification-code\">  " + code + "  </span>\n" +
                    "        </div>\n" +
                    "        <p>이 코드는 10분 동안 유효합니다.</p>\n" +
                    "        <p class=\"instruction\">코드를 복사하여 앱에 붙여넣어 주세요.</p>\n" +
                    "    </div>\n" +
                    "    <div class=\"footer\">\n" +
                    "        <p>이 메일은 발신 전용입니다. 문의사항은 고객센터를 이용해 주세요.</p>\n" +
                    "        <p>&copy; 2025 DummyTalk. All Rights Reserved.</p>\n" +
                    "    </div>\n" +
                    "</div>\n" +
                    "</body>\n" +
                    "</html>", true);
            helper.setReplyTo("no-reply@mail.com");
        } catch (MessagingException e) {
            throw new UserHandler(ErrorCode.CANT_MAKE_EMAIL);
        }

        try {
            mailSender.send(msg);
            emailRepository.save(EmailConverter.toNewEmail(email, code, expireTime)); /// Redis를 사용한 자체 TimeOut 세팅할 것
        } catch (RuntimeException e) {
            throw new UserHandler(ErrorCode.CANT_SEND_EMAIL);
        }
    }

    @Override
    public void verifyEmail(UserRequestDTO.VerificationRequestDTO requestDTO) {
        Optional<Email> verification = emailRepository.findByEmailAndCode(requestDTO.getEmail(), requestDTO.getCode());

        if (verification.isEmpty()) {
            throw new UserHandler(ErrorCode.WRONG_EMAIL_CODE);
        }
        long hourDiff = ChronoUnit.HOURS.between(LocalDateTime.now(), verification.get().getExpireTime());
        if (hourDiff >= 24) {
            throw new UserHandler(ErrorCode.EMAIL_EXPIRED);
        }
    }

    @Override
    @Transactional
    public void signIn(UserRequestDTO.SignInRequestDTO request) {

        User user = UserConverter.toNewUser(request);
        User savedUser = userRepository.save(user);

        Info info = Info.builder()
                .user(savedUser)
                .isSubscribe(false)
                .reqCount(0)
                .build();
        Info savedInfo = infoRepository.save(info);
        user.setInfo(savedInfo);
    }


    @Override
    @Transactional
    public UserResponseDTO.LoginSuccessDTO login(UserRequestDTO.LoginRequestDTO requestDTO) {


        User user = userRepository.findByEmailAndPassword(requestDTO.getEmail(), requestDTO.getPassword()).orElseThrow(() -> new UserHandler(ErrorCode.CANT_FIND_USER));

        user.setLastLogin(LocalDateTime.now());

        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword(), authorities);

        JwtToken jwtToken = jwtProvider.generateToken(authentication);

        return UserResponseDTO.LoginSuccessDTO.builder()
                .username(user.getUsername())
                .accessToken(jwtToken.getAccessToken())
                .build();
    }

    public static String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());

            code.append(CHARACTERS.charAt(randomIndex));
        }

        return code.toString();
    }

    @Override
    @Transactional
    public void withdraw(String email) {

        // User와 관계된 엔티티 먼저 제거
        userQuizRepository.deleteByEmail(email);
        infoRepository.deleteByEmail(email);

        userRepository.deleteByEmail(email); // HARD DELETE!
    }


    public List<UserResponseDTO.GetUserResponseDTO> getAllData() {
        List<UserResponseDTO.GetUserResponseDTO> dtoList = new ArrayList<>();
        userRepository.findAllJoinFetchInfo().forEach(user ->
        dtoList.add(UserResponseDTO.GetUserResponseDTO.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .reqCount(user.getInfo().getReqCount())
                .isSubscribe(user.getInfo().getIsSubscribe())
                .subsExprDate(user.getInfo().getSubsExprDate())
                .build())
        );

        log.info("data count : {}",  dtoList.size());
        return dtoList;
    }
}
