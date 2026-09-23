package DummyTalk.DummyTalk_BE.domain.entity.mapping;

import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.Member;
import DummyTalk.DummyTalk_BE.domain.entity.common.CommonEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
/*
 * member_id 인덱스 추가
 * - MySQL 과 달리 FK 컬럼에 인덱스를 자동 생성 X
 * - countByMemberID -> 모든 테이블을 탐색하게 됨.
 * - ddl-auto: update 가 기동 시 생성한다.
 */
@Table(indexes = @Index(name = "idx_member_dummy_member_id", columnList = "member_id"))
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class MemberDummy extends CommonEntity {

    // 사용자가 조회한 Dummy 기록용 매핑 테이블

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dummy_id")
    private Dummy dummy;

    public static MemberDummy generateMemberDummy(Member member, Dummy dummy) {
        return MemberDummy.builder().member(member).dummy(dummy).build();
    }
}
