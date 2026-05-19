package DummyTalk.DummyTalk_BE.domain.repository.jpa;

import DummyTalk.DummyTalk_BE.domain.entity.Badge;
import DummyTalk.DummyTalk_BE.domain.entity.Member;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberBadgeRepository extends JpaRepository<MemberBadge, Long> {
    boolean existsByMemberAndBadge(Member member, Badge badge);
    List<MemberBadge> findByMember(Member member);

    // Member cascade 범위 밖 → Member 삭제 전 FK 위반 방지를 위해 선행 삭제
    // MemberDummy - Member 매핑하였으니 확인할 것
}