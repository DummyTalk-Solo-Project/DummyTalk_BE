package DummyTalk.DummyTalk_BE.dummy;

import DummyTalk.DummyTalk_BE.domain.entity.User;
import DummyTalk.DummyTalk_BE.domain.repository.UserRepository;
import DummyTalk.DummyTalk_BE.domain.service.dummy.impl.DummyServiceImplV3;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.UserHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
@Slf4j
public class DummyServiceSolveTest {

    @Autowired
    private DummyServiceImplV3 dummyService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    private static final String TEST_EMAIL = "user1";
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_QUIZ_ID = 1L;
    private static final String QUIZ_HASH_KEY = "quiz";
    private static final String QUIZ_ANSWER_LIST_KEY = "quiz:answer";


    @Test
    @DisplayName("문제 풀이 테스트")
    public void solveQuizTest(){
        User user = userRepository.findByEmail(TEST_EMAIL).orElseThrow(() -> new UserHandler(ErrorCode.CANT_FIND_USER));

        dummyService.solveQuiz(user.getEmail(), TEST_QUIZ_ID, 1);
    }
}
