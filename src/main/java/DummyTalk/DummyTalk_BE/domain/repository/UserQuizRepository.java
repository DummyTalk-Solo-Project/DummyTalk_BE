package DummyTalk.DummyTalk_BE.domain.repository;

import DummyTalk.DummyTalk_BE.domain.entity.mapping.User_Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserQuizRepository extends JpaRepository<User_Quiz, Long> {

    @Query("SELECT uq FROM User_Quiz uq WHERE uq.user.id = :userId ORDER BY uq.createdAt LIMIT 1")
    Optional<User_Quiz> findLastestQuizByUserId(Long userId, Integer limit);
}
