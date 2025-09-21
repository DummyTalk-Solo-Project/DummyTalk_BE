package DummyTalk.DummyTalk_BE.domain.service.dummy;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyResponseDTO;
import DummyTalk.DummyTalk_BE.domain.entity.User;

import java.time.LocalDateTime;

public interface DummyService {

    String GetDummyDateForNormal(User user, DummyRequestDTO.RequestInfoDTO requestInfoDTO);

    void openQuiz(User user, LocalDateTime openQuizDate);

    DummyResponseDTO.GetQuizInfoResponseDTO getQuiz(User user);

    void solveQuiz(User user, Long quizId,Integer answer);

}
