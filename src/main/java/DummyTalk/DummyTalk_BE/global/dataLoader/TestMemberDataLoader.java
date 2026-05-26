package DummyTalk.DummyTalk_BE.global.dataLoader;

import DummyTalk.DummyTalk_BE.domain.entity.Info;
import DummyTalk.DummyTalk_BE.domain.entity.Member;
import DummyTalk.DummyTalk_BE.domain.entity.constant.Login;
import DummyTalk.DummyTalk_BE.domain.entity.constant.MemberRole;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.InfoRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * K6 부하 테스트용 테스트 유저 사전 적재 (test1~200@test.com / Test1234!)
 *
 * 활성화: application.yml 의 test.load-users=true 로 설정 (기본 false)
 *
 * ── EC2 DB 시딩 방법 ────────────────────────────────────────────────────────
 *
 * [방법 A] 로컬에서 EC2 DB를 직접 타겟으로 기동 (권장)
 *   IntelliJ Run Configuration > Environment Variables 에 아래 항목 추가:
 *     DB_HOST=<EC2_IP>
 *     REDIS_HOST=<EC2_IP>
 *     ES_URL=http://<EC2_IP>:9200
 *     + JWT_SECRET, MAIL_* 등 나머지 필수 env
 *   application.yml: test.load-users=true
 *   → "[TestMemberDataLoader] - 테스트 유저 200명 생성 완료" 확인 후 앱 종료
 *
 * [방법 B] EC2 서버에서 직접 실행
 *   docker/docker-compose.yml spring 서비스 env 에 일시 추가:
 *     - TEST_LOAD_USERS=true
 *   docker compose restart spring → 로그 확인 → env 제거 → docker compose restart spring
 *
 * ── 멱등성 ────────────────────────────────────────────────────────────────────
 * 이미 존재하는 이메일은 건너뜀. test.load-users=false 로 되돌린 후 재배포해도 데이터 유지.
 */
@Slf4j
@Component
@Order(2) // DummyDataLoader 이후 실행
@RequiredArgsConstructor
@ConditionalOnProperty(name = "test.load-users", havingValue = "true", matchIfMissing = false)
public class TestMemberDataLoader implements ApplicationRunner {

    private static final int TEST_USER_COUNT = 200;
    private static final String TEST_PASSWORD = "Test1234!";
    private static final String EMAIL_FORMAT = "test%d@test.com";
    private static final String USERNAME_FORMAT = "TestUser%d";

    private final MemberRepository memberRepository;
    private final InfoRepository infoRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 이미 존재하는 테스트 유저 이메일 조회 (중복 방지)
        Set<String> existingEmails = memberRepository.findAll().stream()
                .map(Member::getEmail)
                .filter(email -> email.matches("test\\d+@test\\.com"))
                .collect(Collectors.toSet());

        List<String> toCreate = IntStream.rangeClosed(1, TEST_USER_COUNT)
                .mapToObj(i -> String.format(EMAIL_FORMAT, i))
                .filter(email -> !existingEmails.contains(email))
                .toList();

        if (toCreate.isEmpty()) {
            log.info("[TestMemberDataLoader] - 테스트 유저 {}명 이미 존재, 건너뜀", TEST_USER_COUNT);
            return;
        }

        // BCrypt는 의도적으로 느리므로 동일 비밀번호는 한 번만 인코딩
        String encodedPassword = bCryptPasswordEncoder.encode(TEST_PASSWORD);
        // EC2 DB 시딩 시 k6/setup.sql 의 {BCRYPT_HASH} 자리에 아래 해시를 복사해서 사용할 것
        log.info("[TestMemberDataLoader] - BCrypt 해시 (k6/setup.sql 용): {}", encodedPassword);
        log.info("[TestMemberDataLoader] - 테스트 유저 {}명 생성 시작 (기존 {}명 제외)", toCreate.size(), existingEmails.size());

        List<Member> members = new ArrayList<>();
        for (String email : toCreate) {
            int num = Integer.parseInt(email.replaceAll("\\D", ""));
            members.add(Member.builder()
                    .email(email)
                    .password(encodedPassword)
                    .memberName(String.format(USERNAME_FORMAT, num))
                    .login(Login.NORMAL)
                    .role(MemberRole.MEMBER)
                    .build());
        }

        List<Member> savedMembers = memberRepository.saveAll(members);

        // Info 레코드 & Redis pity 해시 초기화
        List<Info> infoList = new ArrayList<>();
        for (Member member : savedMembers) {
            infoList.add(Info.builder()
                    .member(member)
                    .isSubscribe(false)
                    .reqCount(0)
                    .build());

            // 신규 멤버의 천장 스택 초기화 (getDummy와 동일 구조)
            redisTemplate.opsForHash().putAll(
                    "pity:" + member.getId(),
                    Map.of("RARE", "0", "EPIC", "0")
            );
        }

        infoRepository.saveAll(infoList);

        log.info("[TestMemberDataLoader] - 테스트 유저 {}명 생성 완료", savedMembers.size());
    }
}