package DummyTalk.DummyTalk_BE.domain.repository.jpa;

import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberDummy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MemberDummyRepository extends JpaRepository<MemberDummy, Long> {

    @Query("select d from Dummy d join fetch d.rarity where d.id in :idList")
    Page<Dummy> findAllByDummyIdList (@Param("idList") List<Long> idList, Pageable pageable);

    long countMemberDummyByCreatedAtBetween(LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);


    @Query("select md.id from MemberDummy md where md.createdAt between :createdAtAfter and :createdAtBefore")
    List<Long> findAllByCreatedAtToday (@Param("createdAtAfter") LocalDateTime createdAtAfter, @Param("createdAtBefore") LocalDateTime createdAtBefore);
}
