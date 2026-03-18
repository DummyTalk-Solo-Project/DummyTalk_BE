package DummyTalk.DummyTalk_BE.domain.service.member.impl;

import DummyTalk.DummyTalk_BE.domain.converter.UserConverter;
import DummyTalk.DummyTalk_BE.domain.dto.member.MemberRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.member.MemberResponseDTO;
import DummyTalk.DummyTalk_BE.domain.entity.*;
import DummyTalk.DummyTalk_BE.domain.repository.InfoRepository;
import DummyTalk.DummyTalk_BE.domain.repository.MemberQuizRepository;
import DummyTalk.DummyTalk_BE.domain.repository.MemberRepository;
import DummyTalk.DummyTalk_BE.domain.service.member.MemberService;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.UserHandler;
import DummyTalk.DummyTalk_BE.global.security.jwt.JWTProvider;
import DummyTalk.DummyTalk_BE.global.security.jwt.JwtToken;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final JavaMailSender mailSender;
    private final JWTProvider jwtProvider;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 4;
    private final InfoRepository infoRepository;
    private final MemberQuizRepository memberQuizRepository;
    private final RedisTemplate<String, Object> redisTemplate;

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
                    "        <p>이 코드는 3분 동안 유효합니다.</p>\n" +
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

        // 만료 전 재요청 시 거절 로직
        if (redisTemplate.opsForValue().get("email:"+email) != null){
            throw new UserHandler(ErrorCode.ALREADY_SEND);
        }

        if(memberRepository.findByEmail(email).isPresent()){
            throw new UserHandler(ErrorCode.ALREADY_REGISTERED);
        }

        try {
            mailSender.send(msg);
//            emailRepository.save(EmailConverter.toNewEmail(email, code, expireTime)); /// Redis를 사용한 자체 TimeOut 세팅할 것
            redisTemplate.opsForValue().set("email:"+email, code, 3, TimeUnit.MINUTES); // 3분으로 설정.
            log.info("[EMAIL SENT] code {} -> {}", code, email);
        } catch (RuntimeException e) {
            throw new UserHandler(ErrorCode.CANT_SEND_EMAIL);
        }
    }

    @Override
    public void verifyEmail(MemberRequestDTO.VerificationRequestDTO requestDTO) {
        String code = redisTemplate.opsForValue().get("email:" + requestDTO.getEmail()).toString();

        if (code == null) {
            throw new UserHandler(ErrorCode.EMAIL_EXPIRED);
        }
        log.info("code: {}", code);
        if (!code.equals(requestDTO.getCode()) ) {
            throw new UserHandler(ErrorCode.WRONG_EMAIL_CODE);
        }
        log.info("[EMAIL VERIFIED] email: {},  code: {}",  requestDTO.getEmail(), code);
    }

    @Override
    @Transactional
    public void signIn(MemberRequestDTO.SignInRequestDTO request) {

        Optional<Member> byEmail = memberRepository.findByEmail(request.getEmail());
        if (byEmail.isPresent()){
            throw new UserHandler(ErrorCode.ALREADY_REGISTERED);
        }

        Member member = UserConverter.toNewUser(request);
        Member savedMember = memberRepository.save(member);

        Info info = Info.builder()
                .member(savedMember)
                .isSubscribe(false)
                .reqCount(0)
                .build();
        Info savedInfo = infoRepository.save(info);
        member.setInfo(savedInfo);

        log.info("[SIGNIN] email: {}, password: {}, username: {}", request.getEmail(), request.getPassword(), member.getMemberName());
    }


    @Override
    @Transactional
    public MemberResponseDTO.LoginSuccessDTO login(MemberRequestDTO.LoginRequestDTO requestDTO) {

        Member member = memberRepository.findByEmailAndPassword(requestDTO.getEmail(), requestDTO.getPassword()).orElseThrow(() -> new UserHandler(ErrorCode.CANT_FIND_USER));

        member.setLastLogin(LocalDateTime.now());

        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(member.getEmail(), member.getPassword(), authorities);

        JwtToken jwtToken = jwtProvider.generateToken(authentication);

        log.info("[LOGIN] email: {}", member.getEmail());

        return MemberResponseDTO.LoginSuccessDTO.builder()
                .username(member.getMemberName())
                .accessToken(jwtToken.getAccessToken())
                .build();
    }

    @Override
    @Transactional
    public void withdraw(String email) {

        // User와 관계된 엔티티 먼저 제거
        memberQuizRepository.deleteByEmail(email);
        infoRepository.deleteByEmail(email);

        memberRepository.deleteByEmail(email); // HARD DELETE!

        log.info("[WITHDRAW] email: {}", email);
    }


    public List<MemberResponseDTO.GetUserResponseDTO> getAllData() {
        List<MemberResponseDTO.GetUserResponseDTO> dtoList = new ArrayList<>();
        memberRepository.findAllJoinFetchInfo().forEach(user ->
        dtoList.add(MemberResponseDTO.GetUserResponseDTO.builder()
                .email(user.getEmail())
                .username(user.getMemberName())
                .reqCount(user.getInfo().getReqCount())
                .isSubscribe(user.getInfo().getIsSubscribe())
                .subsExprDate(user.getInfo().getSubsExprDate())
                .build())
        );

        log.info("data count : {}",  dtoList.size());
        return dtoList;
    }
}
