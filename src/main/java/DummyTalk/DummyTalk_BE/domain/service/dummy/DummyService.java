package DummyTalk.DummyTalk_BE.domain.service.dummy;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyResponseDTO;
import DummyTalk.DummyTalk_BE.domain.entity.User;

import java.time.LocalDateTime;

public interface DummyService {

    String GetDummyDateForNormal(User user, DummyRequestDTO.RequestInfoDTO requestInfoDTO);

    void openQuiz(User user, LocalDateTime openQuizDate);

    DummyResponseDTO.GetQuizInfoResponseDTO getQuiz(User user);

    void solveQuiz(User user, Long quizId, Integer answer);

}


/*
 * 동시성 개선을 위한 프로세스
 * (K6, Jmeter 설정 및 코드 작성 후)
 *
 * 1. 기본 Redis 인메모리 활용을 통한 동시성 테스트 -> DummyServiceImpl
 * 2. 퀴즈 조회 로직 -> Redis 캐싱을 통한 빠른 조회 도입 -> DummyServiceImplV2
 * 3. 동시성 관련 로직 or @Async 추가
 * 4. 이후 더욱 개선 방식 고민...?
 *
 * */