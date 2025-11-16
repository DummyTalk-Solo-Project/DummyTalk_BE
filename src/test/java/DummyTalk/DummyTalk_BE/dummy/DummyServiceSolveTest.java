package DummyTalk.DummyTalk_BE.dummy;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.repository.UserRepository;
import DummyTalk.DummyTalk_BE.domain.service.dummy.impl.DummyServiceImplV3;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;

@SpringBootTest
@Slf4j
public class DummyServiceSolveTest {

    @Autowired
    private DummyServiceImplV3 dummyService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    private static final String TEST_EMAIL = "jijysun@naver.com";
    private static final Long TEST_QUIZ_ID = 27L;


    @Test
    @DisplayName("문제 풀이 테스트")
    public void solveQuizTest(){
//        User user = userRepository.findByEmail(TEST_EMAIL).orElseThrow(() ->//        dummyService.solveQuiz(user.getEmail(), TEST_QUIZ_ID, 1); new UserHandler(ErrorCode.CANT_FIND_USER));

        dummyService.solveQuizVer4(DummyRequestDTO.SolveQuizReqDTO.builder()
                .quizId(TEST_QUIZ_ID)
                .email(TEST_EMAIL)
                .answer(1)
                .build());
    }

    @Test
    @DisplayName("문제 오픈 테스트")
    public void openQuizTest(){
        dummyService.openQuiz(TEST_EMAIL, LocalDateTime.of(2021, 11, 1, 12, 1)); // 테스트용 오픈
    }
}
