package DummyTalk.DummyTalk_BE.global.scheduler.task;

import DummyTalk.DummyTalk_BE.domain.entity.DailySettlement;
import DummyTalk.DummyTalk_BE.domain.entity.constant.RarityType;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.DailySettlementRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.InfoRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.MemberDummyRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminTask {

    private final MemberDummyRepository memberDummyRepository;
    private final MemberRepository memberRepository;
    private final InfoRepository infoRepository;
    private final DailySettlementRepository dailySettlementRepository;

    @Transactional
    public void calculateAndSaveDailySettlement() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 동일 날짜 중복 저장 방지 (스케줄러 재기동, 수동 트리거 등 대비)
        if (dailySettlementRepository.findBySettlementDate(yesterday).isPresent()) {
            log.info("[AdminTask - calculateAndSaveDailySettlement()] - 이미 정산 완료된 날짜: {}", yesterday);
            return;
        }

        // 어제 00:00:00 ~ 23:59:57
        LocalDateTime start = yesterday.atStartOfDay();
        LocalDateTime end = yesterday.atStartOfDay().withHour(23).withMinute(59).withSecond(57);

        long totalDummyViews = memberDummyRepository.countMemberDummyByCreatedAtBetween(start, end);
        long newMemberCount = memberRepository.countMemberByCreatedAtBetween(start, end);
        long activeMemberCount = memberRepository.countByIsDeletedFalse();
        long activeSubscriberCount = infoRepository.countActiveSubscribers();

        // MemberDummy → Dummy → Rarity.name GROUP BY 집계 (Bug 2 수정)
        List<Object[]> rarityRows = memberDummyRepository.countByRarityAndCreatedAtBetween(start, end);
        long commonCount = 0, rareCount = 0, epicCount = 0, specialCount = 0;
        for (Object[] row : rarityRows) {
            RarityType name = (RarityType) row[0];
            long count = (Long) row[1];
            switch (name) {
                case COMMON  -> commonCount  = count;
                case RARE    -> rareCount    = count;
                case EPIC    -> epicCount    = count;
                case SPECIAL -> specialCount = count;
                default      -> {} // TEST 등급은 정산 제외
            }
        }

        DailySettlement settlement = DailySettlement.of(
                yesterday,
                totalDummyViews,
                newMemberCount,
                commonCount,
                rareCount,
                epicCount,
                specialCount,
                activeMemberCount,
                activeSubscriberCount
        );
        dailySettlementRepository.save(settlement);

        log.info("[AdminTask - calculateAndSaveDailySettlement()] - 정산 완료 | 날짜={} 총조회={} 신규회원={} " +
                "COMMON={} RARE={} EPIC={} SPECIAL={} 활성회원={} 구독자={}",
                yesterday, totalDummyViews, newMemberCount,
                commonCount, rareCount, epicCount, specialCount,
                activeMemberCount, activeSubscriberCount);
    }
}