package DummyTalk.DummyTalk_BE.domain.repository;

import DummyTalk.DummyTalk_BE.domain.entity.Quiz;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @Query ("SELECT q FROM Quiz q ORDER BY q.createdAt DESC LIMIT 1")
    Quiz findLastestQuiz();

    @Lock(LockModeType.PESSIMISTIC_WRITE) // 쓰기 잠금, 배타적 잠금. 해당
    @Query("select Quiz q From Quiz where q.id = :id")
    Optional<Quiz> findQuizByIdForDecrease(Long id);
}
