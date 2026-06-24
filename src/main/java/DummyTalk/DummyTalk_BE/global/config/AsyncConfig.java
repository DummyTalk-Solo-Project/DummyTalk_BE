package DummyTalk.DummyTalk_BE.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    // 메일 발송 전용
    // 외부 SMTP I/O + Redis BRPOP 블로킹 발생
    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        return new VirtualThreadTaskExecutor("MailVT-");
    }

    // 뱃지 처리 전용
    // 내부 DB 작업, JDBC 대기 시 VT park()
    @Bean(name = "BadgeExecutor")
    public Executor badgeExecutor() {
        return new VirtualThreadTaskExecutor("BadgeVT-");
    }
}
