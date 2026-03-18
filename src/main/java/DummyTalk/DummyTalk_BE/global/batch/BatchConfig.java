package DummyTalk.DummyTalk_BE.global.batch;

import DummyTalk.DummyTalk_BE.domain.entity.Member;
import DummyTalk.DummyTalk_BE.domain.service.email.MailService;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@Slf4j
public class BatchConfig {

    private final EntityManagerFactory emf;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final MailService mailService;

    @Bean
    public Job resetCountJob() {
        return new JobBuilder("resetCountJob", jobRepository)
                .start(userDataStep())
                .build();
    }

    @Bean
    public Step userDataStep(){
        return new StepBuilder("userDataStep", jobRepository)
                .<Member, Member>chunk(100, transactionManager)
                .reader(userDataReader())
                .processor(userDataProcessor())
                .writer(userDataWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<Member> userDataReader() {
        return new JpaPagingItemReaderBuilder<Member>()
                .name("userDataReader")
                .entityManagerFactory(emf)
                .queryString("select u from User u join info i on user.id = i.user.id ")
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<Member, Member> userDataProcessor() {
        return user -> {
            // 1. 카운트 리셋
            user.getInfo().resetReqCount();

            // 2. 메일 발송 로직: 5일 이상 미접속 시
            if (ChronoUnit.DAYS.between(user.getLastLogin(), LocalDateTime.now()) >= 5) {
                try {
                    mailService.sendReminderEmail(user.getEmail(), user.getMemberName());
                } catch (Exception e) {
                    log.error("메일 발송 실패 - 사용자: {}", user.getId(), e);
                }
            }
            return user;
        };
    }

    @Bean
    public JpaItemWriter<Member> userDataWriter() {
        return new JpaItemWriterBuilder<Member>()
                .entityManagerFactory(emf)
                .build();
    }
}