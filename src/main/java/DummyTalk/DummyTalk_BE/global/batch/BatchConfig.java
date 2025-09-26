package DummyTalk.DummyTalk_BE.global.batch;

import DummyTalk.DummyTalk_BE.domain.entity.Info;
import DummyTalk.DummyTalk_BE.domain.entity.User;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
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
public class BatchConfig {

    private final EntityManagerFactory emf;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager  transactionManager;


    @Bean
    public Job resetCountJob(){
        return new JobBuilder("resetCountJob", jobRepository)
                .start(userDataStep())
                .build();
    }

    @Bean
    public Step userDataStep(){
        return new StepBuilder("userDataStep", jobRepository)
                .<User, User>chunk(100, transactionManager)
                .reader(userDataReader())
                .processor(userDataProcessor())
                .writer(userDataWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<User> userDataReader(){
        return new JpaPagingItemReaderBuilder<User>()
                .name("userDataReader")
                .entityManagerFactory(emf)
                .queryString("select u from User u join info i on user.id = i.user.id ")
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<User, User> userDataProcessor(){
        return user -> {
            user.getInfo().resetReqCount();

            if (ChronoUnit.DAYS.between(user.getLastLogin(), LocalDateTime.now()) >= 5){
                // 메세지 발송 로직
            }
            return user;
        };
    }

    @Bean
    public JpaItemWriter<User> userDataWriter(){
        return new JpaItemWriterBuilder<User>()
                .entityManagerFactory(emf)
                .build();
    }
}
