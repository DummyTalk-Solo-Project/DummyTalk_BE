package DummyTalk.DummyTalk_BE.domain.repository.jpa;

import DummyTalk.DummyTalk_BE.domain.entity.Badge;
import DummyTalk.DummyTalk_BE.domain.entity.Member;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberBadgeRepository extends JpaRepository<MemberBadge, Long> {
    boolean existsByMemberAndBadge(Member member, Badge badge);
    List<MemberBadge> findByMember(Member member);
}