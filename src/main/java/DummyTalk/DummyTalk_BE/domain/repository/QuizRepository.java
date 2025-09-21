package DummyTalk.DummyTalk_BE.domain.repository;

import DummyTalk.DummyTalk_BE.domain.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @Query ("SELECT q FROM Quiz q ORDER BY q.createdAt DESC LIMIT 1")
    Quiz findLastestQuiz();
}
