package DummyTalk.DummyTalk_BE.domain.repository;

import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberQuizRepository extends JpaRepository<MemberQuiz, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberQuiz uq  WHERE uq.member.email = :email")
    void deleteByEmail(@Param("email") String email);

    @Query("SELECT uq FROM MemberQuiz uq WHERE uq.member.id = :userId ORDER BY uq.createdAt LIMIT 1")
    Optional<MemberQuiz> findLastestQuizByUserId(Long userId, Integer limit);
}
