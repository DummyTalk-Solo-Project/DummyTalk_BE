package DummyTalk.DummyTalk_BE.domain.repository;

import DummyTalk.DummyTalk_BE.domain.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz,Long> {
}
