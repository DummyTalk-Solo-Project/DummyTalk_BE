package DummyTalk.DummyTalk_BE.domain.repository;

import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberDummy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberDummyRepository extends JpaRepository<MemberDummy, Long> {
}
