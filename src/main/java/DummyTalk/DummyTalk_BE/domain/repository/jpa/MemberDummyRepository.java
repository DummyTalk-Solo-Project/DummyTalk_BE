package DummyTalk.DummyTalk_BE.domain.repository.jpa;

import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberDummy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MemberDummyRepository extends JpaRepository<MemberDummy, Long> {

    @Query("select d from Dummy d join fetch d.rarity where d.id in :idList")
    List<Dummy> findAllByDummyIdList (Collection<Long> idList);

    long countMemberDummyByCreatedAtBetween(LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);


    @Query("select md.id from MemberDummy md where md.createdAt between :createdAtAfter and :createdAtBefore")
    List<Long> findAllByCreatedAtToday (LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);
}
