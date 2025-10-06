package DummyTalk.DummyTalk_BE.domain.repository;

import DummyTalk.DummyTalk_BE.domain.entity.mapping.UserQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserQuizRepository extends JpaRepository<UserQuiz, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserQuiz uq  WHERE uq.user.email = :email")
    void deleteByEmail(@Param("email") String email);

    @Query("SELECT uq FROM UserQuiz uq WHERE uq.user.id = :userId ORDER BY uq.createdAt LIMIT 1")
    Optional<UserQuiz> findLastestQuizByUserId(Long userId, Integer limit);
}
