package DummyTalk.DummyTalk_BE.domain.service.badge;

import DummyTalk.DummyTalk_BE.domain.entity.Badge;
import DummyTalk.DummyTalk_BE.domain.entity.Member;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberBadge;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.BadgeRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.MemberBadgeRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.MemberRepository;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.MemberHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final MemberBadgeRepository memberBadgeRepository;
    private final MemberRepository memberRepository;
    
    public static final String BADGE_FIRST_STEP  = "첫 발걸음";
    public static final String BADGE_REGULAR     = "단골손님";
    public static final String BADGE_ENTHUSIAST  = "열정적인 더미톡유저!";
    public static final String BADGE_PITY        = "운명의 장난";
    public static final String BADGE_LEGEND      = "전설의 탄생";

    /**
     * getDummy 이벤트 기반 뱃지 체크 & 부여
     * BadgeEventListener에서 @Async + 새 트랜잭션으로 호출됨
     */
    @Transactional
    public void checkAndAwardByDummyViewed(Long memberId, String rarityName, Boolean isPityTriggered, long totalDummyCount) {


        List<String> candidates = new ArrayList<>();

        // 누적 조회 횟수 기반 뱃지
        if (totalDummyCount == 1)   candidates.add(BADGE_FIRST_STEP);
        if (totalDummyCount == 10)  candidates.add(BADGE_REGULAR);
        if (totalDummyCount == 100) candidates.add(BADGE_ENTHUSIAST);

        // 천장 발동 뱃지
        if (Boolean.TRUE.equals(isPityTriggered)) candidates.add(BADGE_PITY);

        // SPECIAL 등급 획득 뱃지
        if ("SPECIAL".equals(rarityName)) candidates.add(BADGE_LEGEND);

        // 대상인 지 Member 조회 보다 먼저
        // 안그러면 커넥션 경합에 그대로 기여 + DB 조회 증가!
        if (candidates.isEmpty()) return; // DB 를 치기 전에 빠져나간다 — 대다수 요청이 여기서 끝난다

        Member member = memberRepository.findById(memberId).orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));

        for (String badgeName : candidates) {
            awardIfNotOwned(member, badgeName);
        }
    }
    
    private void awardIfNotOwned(Member member, String badgeName) { // 이미 부여된 경우 SKIP
        Optional<Badge> badgeOpt = badgeRepository.findByName(badgeName);
        if (badgeOpt.isEmpty()) {
            log.warn("[BadgeService - awardIfNotOwned()] 뱃지 '{}' DB에 없음, 건너뜀", badgeName);
            return;
        }
        Badge badge = badgeOpt.get();

        if (memberBadgeRepository.existsByMemberAndBadge(member, badge)) {
            log.debug("[BadgeService - awardIfNotOwned()] Member {} 이미 뱃지 '{}' 보유", member.getId(), badgeName);
            return;
        }

        memberBadgeRepository.save(MemberBadge.createNewMemberBadge(member, badge));
        log.info("[BadgeService - awardIfNotOwned()] 뱃지 '{}' → Member {} 부여 완료", badgeName, member.getId());
    }
}