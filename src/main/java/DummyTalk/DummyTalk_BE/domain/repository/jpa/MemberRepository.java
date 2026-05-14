package DummyTalk.DummyTalk_BE.domain.repository.jpa;

import DummyTalk.DummyTalk_BE.domain.entity.Member;
import jakarta.persistence.LockModeType;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Member u WHERE u.email = :email")
    void deleteByEmail(@Param("email") String email);

    Optional<Member> findByEmail(String email);

    @Query("SELECT u FROM Member u JOIN FETCH Info i ON i.member.id = u.id WHERE u.email = :email")
    Optional<Member>findByEmailFetchInfo(@Param("email") String email);

    @BatchSize(size = 100)
    @Query("SELECT u FROM Member u JOIN FETCH Info i ON i.member.id = u.id")
    List<Member> findAllJoinFetchInfo();

    @Query("SELECT m FROM Member m join FETCH Info i on i.member.id = m.id where m.id = :memberId")
    Optional<Member> findByIdFetchJoinInfo (Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Member u JOIN FETCH Info i ON i.member.id = u.id WHERE u.email = :email")
    Optional<Member>findByEmailFetchInfoWithLock(@Param("email") String email);

    Optional<Member> findByEmailAndPassword(String email, String password);

    boolean existsByEmail(String mail);

    long countMemberByCreatedAtBetween(LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);

    // 탈퇴 후 2주 초과 → 영구 삭제 대상 조회
    @Query("SELECT m FROM Member m WHERE m.isDeleted = true AND m.deletedAt < :cutoff")
    List<Member> findAllExpiredMembers(@Param("cutoff") LocalDateTime cutoff);
}
