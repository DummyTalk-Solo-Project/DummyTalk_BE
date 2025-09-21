package DummyTalk.DummyTalk_BE.domain.repository;

import DummyTalk.DummyTalk_BE.domain.entity.mapping.UserQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserQuizRepository extends JpaRepository<UserQuiz, Long> {

    @Query("SELECT uq FROM UserQuiz uq WHERE uq.user.id = :userId ORDER BY uq.createdAt LIMIT 1")
    Optional<UserQuiz> findLastestQuizByUserId(Long userId, Integer limit);
}
