package DummyTalk.DummyTalk_BE;

import DummyTalk.DummyTalk_BE.domain.entity.User;
import DummyTalk.DummyTalk_BE.domain.repository.UserRepository;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.batch.BatchConfig;
import DummyTalk.DummyTalk_BE.global.exception.handler.UserHandler;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;


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

        log.info("Batch Job Complete, user : {}", user.getInfo().getReqCount());
    }

    /*@TestConfiguration
    public static class BatchTestConfig {
        @Bean
        public JobLauncherTestUtils jobLauncherTestUtils() {
            return new JobLauncherTestUtils();
        }
    }*/
}


