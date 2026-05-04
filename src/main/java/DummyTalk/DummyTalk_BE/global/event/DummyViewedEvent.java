package DummyTalk.DummyTalk_BE.global.event;

import lombok.Getter;



/**
 * getDummy() 이후 비동기 뱃지 체크 이벤트
 * totalDummyCount는 트랜잭션 내에서 미리 계산해 전달 → 리스너에서 재조회 불필요
 * */
@Getter
public class DummyViewedEvent {
    
    private final Long memberId;
    private final String rarityName;       
    private final Boolean isPityTriggered; 
    private final long totalDummyCount;  

    public DummyViewedEvent(Long memberId, String rarityName, Boolean isPityTriggered, long totalDummyCount) {
        this.memberId = memberId;
        this.rarityName = rarityName;
        this.isPityTriggered = isPityTriggered;
        this.totalDummyCount = totalDummyCount;
    }
}