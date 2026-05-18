package DummyTalk.DummyTalk_BE.domain.dto.admin;

import DummyTalk.DummyTalk_BE.domain.entity.DailySettlement;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

public class AdminRespDTO {

    @Getter
    @Builder
    public static class DailySettlementRespDTO {
        private LocalDate settlementDate;
        private Long totalDummyViews;
        private Long newMemberCount;
        private Long commonCount;
        private Long rareCount;
        private Long epicCount;
        private Long specialCount;
        private Long activeMemberCount;
        private Long activeSubscriberCount;

        public static DailySettlementRespDTO from(DailySettlement s) {
            return DailySettlementRespDTO.builder()
                    .settlementDate(s.getSettlementDate())
                    .totalDummyViews(s.getTotalDummyViews())
                    .newMemberCount(s.getNewMemberCount())
                    .commonCount(s.getCommonCount())
                    .rareCount(s.getRareCount())
                    .epicCount(s.getEpicCount())
                    .specialCount(s.getSpecialCount())
                    .activeMemberCount(s.getActiveMemberCount())
                    .activeSubscriberCount(s.getActiveSubscriberCount())
                    .build();
        }
    }
}