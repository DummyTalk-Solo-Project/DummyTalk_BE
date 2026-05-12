package DummyTalk.DummyTalk_BE.domain.service.member;

import DummyTalk.DummyTalk_BE.domain.dto.member.MemberReqDTO;
import DummyTalk.DummyTalk_BE.domain.dto.member.MemberRespDTO;
import DummyTalk.DummyTalk_BE.domain.service.email.EMailService;
import DummyTalk.DummyTalk_BE.domain.entity.*;
import DummyTalk.DummyTalk_BE.domain.entity.constant.Login;
import DummyTalk.DummyTalk_BE.domain.entity.constant.MemberRole;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberBadge;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.InfoRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.MemberBadgeRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.MemberQuizRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.MemberRepository;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.MemberHandler;
import DummyTalk.DummyTalk_BE.global.security.jwt.JWT;
import DummyTalk.DummyTalk_BE.global.security.jwt.JWTProvider;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final JavaMailSender mailSender;
    private final JWTProvider jwtProvider;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final EMailService emailService;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 4;
    private final InfoRepository infoRepository;
    private final MemberQuizRepository memberQuizRepository;
    private final MemberBadgeRepository memberBadgeRepository;
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

    @Transactional(readOnly = true)
    public Boolean checkEmailDuplicate(String email) {
        Optional<Member> byEmail = memberRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            log.info("[MemberService - checkEmailDuplicate()] - duplicate email {}", email);
            throw new MemberHandler(ErrorCode.EXIST_MEMBER);
        } else {
            log.info("[MemberService - checkEmailDuplicate()] - Success to check duplicate {}", email);
            return true;
        }
    }

    public void requestVerificationCode(String email) {
        String code = generateVerificationCode();
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent(email, code, Duration.ofMinutes(3));

        if (Boolean.FALSE.equals(ifAbsent)) {
            log.info("[MemberService - requestVerificationCode()] - already send to {} with {}", email, redisTemplate.opsForValue().get(email));
            throw new MemberHandler(ErrorCode.ALREADY_SEND); // 따닥 방지
        } else {
            redisTemplate.opsForList().leftPush("email_queue", email + ":" + code);
            log.info("[MemberService - requestVerificationCode()] - call EmailService with {} - {}", email, code);
//            emailService.startWork();
        }
    }

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
            throw new MemberHandler(ErrorCode.CANT_MAKE_EMAIL);
        }

        // 만료 전 재요청 시 거절 로직
        if (redisTemplate.opsForValue().get("email:" + email) != null) {
            throw new MemberHandler(ErrorCode.ALREADY_SEND);
        }

        if (memberRepository.findByEmail(email).isPresent()) {
            throw new MemberHandler(ErrorCode.ALREADY_REGISTERED);
        }

        try {
            mailSender.send(msg);
            redisTemplate.opsForValue().set("email:" + email, code, 3, TimeUnit.MINUTES); // 3분으로 설정.
            log.info("[MemberService - sendVerificationEmail()] - code {} -> {}", code, email);
        } catch (RuntimeException e) {
            throw new MemberHandler(ErrorCode.CANT_SEND_EMAIL);
        }
    }

    public void verifyEmail(MemberReqDTO.VerificationRequestDTO requestDTO) {
        String code = redisTemplate.opsForValue().get("email:" + requestDTO.getEmail()).toString();

        if (code == null) {
            throw new MemberHandler(ErrorCode.EMAIL_EXPIRED);
        }
        log.info("[MemberService - verifyEmail()] - code: {}", code);
        if (!code.equals(requestDTO.getCode())) {
            throw new MemberHandler(ErrorCode.WRONG_EMAIL_CODE);
        }
        log.info("[MemberService - verifyEmail()] - email: {}, code: {}", requestDTO.getEmail(), code);
    }

    @Transactional
    public void signIn(MemberReqDTO.SignInRequestDTO request) {

        Optional<Member> byEmail = memberRepository.findByEmail(request.getEmail());
        if (byEmail.isPresent()) {
            throw new MemberHandler(ErrorCode.ALREADY_REGISTERED);
        }

        Member savedMember = memberRepository.save(Member.builder()
                .memberName(request.getUsername())
                .email(request.getEmail())
                .password(bCryptPasswordEncoder.encode(request.getPassword()))
                .login(Login.NORMAL)
                        .role(MemberRole.MEMBER)
                .build());

        Info info = Info.builder()
                .member(savedMember)
                .isSubscribe(false)
                .reqCount(0)
                .build();
        Info savedInfo = infoRepository.save(info);

        Map<String, String> initialPity = new HashMap<>();
        initialPity.put("RARE", "0");
        initialPity.put("EPIC", "0");
        redisTemplate.opsForHash().putAll("pity:" + savedMember.getId(), initialPity);


        savedMember.setInfo(savedInfo);

        log.info("[MemberService - signIn()] - email: {}, username: {}", request.getEmail(), savedMember.getMemberName());
    }

    @Transactional
    public MemberRespDTO.MemberInfoDTO login(MemberReqDTO.LoginRequestDTO dto) {

        Member member = memberRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));
        if (!bCryptPasswordEncoder.matches(dto.getPassword(), member.getPassword())) {
            throw new MemberHandler(ErrorCode.MEMBER_NOT_FOUND);
        }

        // 탈퇴 회원 분기: 2주 이내 → 복구 유도 / 2주 초과 → 없는 계정으로 처리
        if (member.isWithdrawn()) {
            boolean restorable = member.getDeletedAt().isAfter(LocalDateTime.now().minusWeeks(2));
            if (restorable) {
                log.info("[MemberService - login()] - 탈퇴 후 2주 이내 로그인 시도 (복구 가능): {}", dto.getEmail());
                throw new MemberHandler(ErrorCode.MEMBER_WITHDRAWN_RESTORABLE);
            } else {
                log.info("[MemberService - login()] - 탈퇴 후 2주 초과 로그인 시도: {}", dto.getEmail());
                throw new MemberHandler(ErrorCode.MEMBER_NOT_FOUND);
            }
        }

        member.setLastLogin(LocalDateTime.now());

        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(member.getRole().toString()));
        Authentication authentication = new UsernamePasswordAuthenticationToken(member.getEmail(), member.getPassword(), authorities);
        JWT jwt = jwtProvider.generateToken(authentication);

        redisTemplate.opsForValue().set("refresh:"+dto.getEmail(), jwt.getRefreshToken());

        log.info("[MemberService - login()] - Success to login -> {} - {}", dto.getEmail(), jwt.getRefreshToken());

        return MemberRespDTO.MemberInfoDTO.builder().jwt(jwt).username(member.getMemberName()).build();
    }

    @Transactional
    public MemberRespDTO.MemberInfoDTO restoreAccount(MemberReqDTO.LoginRequestDTO dto) {

        Member member = memberRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));
        if (!bCryptPasswordEncoder.matches(dto.getPassword(), member.getPassword())) {
            throw new MemberHandler(ErrorCode.MEMBER_NOT_FOUND);
        }
        if (!member.isWithdrawn()) {
            throw new MemberHandler(ErrorCode.MEMBER_NOT_FOUND);
        }

        boolean restorable = member.getDeletedAt().isAfter(LocalDateTime.now().minusWeeks(2));
        if (!restorable) {
            throw new MemberHandler(ErrorCode.MEMBER_WITHDRAWN_EXPIRED);
        }

        member.restore();
        member.setLastLogin(LocalDateTime.now());

        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(member.getRole().toString()));
        Authentication authentication = new UsernamePasswordAuthenticationToken(member.getEmail(), member.getPassword(), authorities);
        JWT jwt = jwtProvider.generateToken(authentication);

        redisTemplate.opsForValue().set("refresh:" + dto.getEmail(), jwt.getRefreshToken());

        log.info("[MemberService - restoreAccount()] - 계정 복구 완료: {}", dto.getEmail());

        return MemberRespDTO.MemberInfoDTO.builder().jwt(jwt).username(member.getMemberName()).build();
    }

    public void logout(String accessToken, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));
        redisTemplate.delete("refresh:" + member.getEmail());

        Long remainingTime = jwtProvider.getRemainingTime(accessToken); // TTL 용 남은 시간 계산

        if (remainingTime > 0) {
            String key = "blacklist:" + accessToken;
            redisTemplate.opsForValue().set(key, "logout", remainingTime, TimeUnit.MILLISECONDS);
            log.info("[MemberService - logout()] - add AccessToken in BlackList! remainingTime: {}ms", remainingTime);
        }

        log.info("[MemberService - logout()] - Success to logout -> {}", member.getEmail());
    }

    public MemberRespDTO.FindEmailRespDTO findEmail(String email) {
        boolean existsByEmail = memberRepository.existsByEmail(email);

        if (existsByEmail) {
            return MemberRespDTO.FindEmailRespDTO.builder().email(email).build();
        } else {
            throw new MemberHandler(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Transactional
    public void withdraw(Long memberId) {

        // SOFT DELETE
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));
        member.withdraw(); // softDelete() — isDeleted=true, deletedAt=now()

        // RT 무효화
        redisTemplate.delete("refresh:" + member.getEmail());

        log.info("[MemberService - withdraw()] - SoftDelete 처리: {}", member.getEmail());
    }


    @Transactional
    public void resetPassword(String email) {
        findEmail(email);

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));

        String tempPassword = generateVerificationCode();
        member.changePassword(bCryptPasswordEncoder.encode(tempPassword));

        emailService.sendPasswordResetEmailAsync(email, tempPassword); // @Async - 비동기 처리

        log.info("[MemberService - resetPassword()] - 임시 비밀번호 발급: {}", email);
    }

    @Transactional(readOnly = true)
    public MemberRespDTO.GetMemberResponseDTO getMyData(Long memberId) {
        Member member = memberRepository.findByIdFetchJoinInfo(memberId).orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));

        // 등급별 천장 스택 조회
        String pityKey = "pity:" + memberId;
        Map<Object, Object> pity = redisTemplate.opsForHash().entries(pityKey);
        int commonStack = Integer.parseInt(pity.getOrDefault("COMMON", "0").toString());
        int rareStack = Integer.parseInt(pity.getOrDefault("RARE", "0").toString());
        int epicStack = Integer.parseInt(pity.getOrDefault("EPIC", "0").toString());
        
        List<MemberBadge> memberBadges = memberBadgeRepository.findByMember(member);
        List<MemberRespDTO.BadgeDTO> badgeDTOList = memberBadges.stream()
                .map(mb -> MemberRespDTO.BadgeDTO.builder()
                        .name(mb.getBadge().getName())
                        .content(mb.getBadge().getContent())
                        .imageUrl(mb.getBadge().getImageUrl())
                        .acquiredAt(mb.getCreatedAt())
                        .build())
                .toList();

        MemberRespDTO.GetMemberResponseDTO dto = MemberRespDTO.GetMemberResponseDTO.builder()
                .email(member.getEmail())
                .memberName(member.getMemberName())
                .reqCount(member.getInfo().getReqCount())
                .isSubscribe(member.getInfo().getIsSubscribe())
                .subsExprDate(member.getInfo().getSubsExprDate())
                .commonStack(commonStack)
                .rareStack(rareStack)
                .epicStack(epicStack)
                .badgeList(badgeDTOList)
                .build();

        log.info("[MemberService - getMyData()] - dto: {}", dto);
        return dto;
    }
}
