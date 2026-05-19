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
    
    long countByMember_Id(Long memberId); // 뱃지 조건 체크용 특정 사용자의 누적 더미 조회 횟수

    // 날짜 범위 내 MemberDummy → Dummy → Rarity 연결로 등급별 조회 수 집계
    @Query("SELECT d.rarity.name, COUNT(md) FROM MemberDummy md JOIN md.dummy d " +
           "WHERE md.createdAt BETWEEN :start AND :end GROUP BY d.rarity.name")
    List<Object[]> countByRarityAndCreatedAtBetween(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
