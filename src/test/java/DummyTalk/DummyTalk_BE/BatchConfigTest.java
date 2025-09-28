package DummyTalk.DummyTalk_BE;

import DummyTalk.DummyTalk_BE.domain.entity.User;
import DummyTalk.DummyTalk_BE.domain.repository.UserRepository;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.UserHandler;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
@SpringBatchTest
@Slf4j
public class BatchConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testBatchJob() throws Exception {
        // given -> nothing

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then
        User user = userRepository.findByEmailFetchInfo("jijysun@naver.com").orElseThrow(() -> new UserHandler(ErrorCode.CANT_FIND_USER));

        Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        log.info("Batch Job Complete, {} : {}", user.getEmail(), user.getInfo().getReqCount());
    }
}


