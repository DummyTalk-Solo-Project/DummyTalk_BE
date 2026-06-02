package DummyTalk.DummyTalk_BE.domain.service.badge;

import DummyTalk.DummyTalk_BE.global.event.DummyViewedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BadgeEventListener {

    private final BadgeService badgeService;

    
    
    /*
    * 
    * DummyViewedEvent 수신 → 뱃지 부여 처리
    * 어쨌든 최종적인 EventListener 사용은 getDummy 응답 속도에 영향을 주지 않기 위해...
    * 
    * */
    @Async("BadgeExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) //  MemberDummy 영속 완료 시점으로 이동
    public void onDummyViewed(DummyViewedEvent event) {
        log.info("[BadgeEventListener] - 이벤트 발생! memberId={}, rarity={}, pity={}, totalCount={}",
                event.getMemberId(), event.getRarityName(), event.getIsPityTriggered(), event.getTotalDummyCount());
        try {
            badgeService.checkAndAwardByDummyViewed(
                    event.getMemberId(),
                    event.getRarityName(),
                    event.getIsPityTriggered(),
                    event.getTotalDummyCount()
            );
        } catch (Exception e) { // 일단 not Throwing. 로그로써만.
            log.error("[BadgeEventListener] 뱃지 처리 오류 - memberId={}: {}", event.getMemberId(), e.getMessage(), e);
        }
    }
}