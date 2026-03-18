package DummyTalk.DummyTalk_BE.domain.entity.mapping;

import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.Member;
import DummyTalk.DummyTalk_BE.domain.entity.common.CommonEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class MemberDummy extends CommonEntity {

    // 사용자가 조회한 Dummy 기록용 매핑 테이블

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dummy_id")
    private Dummy dummy;

    public static MemberDummy generateMemberDummy(Member member, Dummy dummy) {
        return MemberDummy.builder().member(member).dummy(dummy).build();
    }
}
