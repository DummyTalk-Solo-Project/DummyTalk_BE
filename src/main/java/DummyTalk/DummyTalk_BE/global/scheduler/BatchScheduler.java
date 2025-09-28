package DummyTalk.DummyTalk_BE.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job job;

    @Scheduled(cron = "0 0 1 * * *")
    public void runUserDataCleanUpJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("runAt", LocalDateTime.now().toString())
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(job, jobParameters);

            log.info("{} ->  execution status: {}", job.getName(), jobExecution.getStatus());

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}