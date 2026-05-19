package DummyTalk.DummyTalk_BE.domain.entity;

import DummyTalk.DummyTalk_BE.domain.entity.common.CommonEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DailySettlement extends CommonEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 정산 날짜 (어제 기준) — 중복 저장 방지를 위해 UNIQUE
    @Column(nullable = false, unique = true)
    private LocalDate settlementDate;

    // 어제 총 Dummy 조회 수 (MemberDummy 생성 건수)
    @Column(nullable = false)
    private Long totalDummyViews;

    // 어제 신규 가입자 수
    @Column(nullable = false)
    private Long newMemberCount;

    // 어제 등급별 Dummy 조회 수
    @Column(nullable = false)
    private Long commonCount;

    @Column(nullable = false)
    private Long rareCount;

    @Column(nullable = false)
    private Long epicCount;

    @Column(nullable = false)
    private Long specialCount;

    // 정산 시점 기준 현황 스냅샷
    @Column(nullable = false)
    private Long activeMemberCount;      // 활성 회원 총 수 (isDeleted=false)

    @Column(nullable = false)
    private Long activeSubscriberCount;  // 현재 구독자 수

    public static DailySettlement of(
            LocalDate settlementDate,
            long totalDummyViews,
            long newMemberCount,
            long commonCount,
            long rareCount,
            long epicCount,
            long specialCount,
            long activeMemberCount,
            long activeSubscriberCount
    ) {
        return DailySettlement.builder()
                .settlementDate(settlementDate)
                .totalDummyViews(totalDummyViews)
                .newMemberCount(newMemberCount)
                .commonCount(commonCount)
                .rareCount(rareCount)
                .epicCount(epicCount)
                .specialCount(specialCount)
                .activeMemberCount(activeMemberCount)
                .activeSubscriberCount(activeSubscriberCount)
                .build();
    }
}