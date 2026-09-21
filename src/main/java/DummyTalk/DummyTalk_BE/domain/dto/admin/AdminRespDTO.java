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

    // K6 부하 테스트 회차 헤더용 — 결과 파일에 "어느 서버 설정에서 측정했나"를 남기기 위한 스냅샷
    @Getter
    @Builder
    public static class LoadTestStateDTO {
        private Long testUserCount;      // test%@test.com 유저 수 (k6 유저 풀 상한)
        private Long reqCountSum;        // 테스트 유저 reqCount 합 — k6 setup/teardown 델타로 Lost Update 판정
        private Integer getDummyVersion; // concurrency.getDummy-version (1 순수TX / 2 분산락 / 3 인터셉터)
        private Boolean interceptorEnabled;
        private Integer hikariPoolSize;  // spring.datasource.hikari.maximum-pool-size
        private Boolean virtualThreads;  // spring.threads.virtual.enabled
    }
}