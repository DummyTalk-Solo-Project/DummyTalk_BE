package DummyTalk.DummyTalk_BE.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 기본값: spring.threads.virtual.enabled(=VIRTUAL_THREADS)
     */
    private static final String ASYNC_VT = "${concurrency.async-virtual-threads:true}";

    // 메일 발송 전용 — 외부 SMTP I/O 가 느려 플랫폼 모드에서는 pool 을 크게 잡는다.
    // EMailService.startMailWorker() 가 BRPOP 으로 무한 블로킹하며 1개를 영구 점유하는데,
    // 원래 설계가 그랬고 측정 경로 밖이라 core 15 면 충분
    @Bean(name = "mailExecutor")
    public Executor mailExecutor(@Value(ASYNC_VT) boolean virtualThreads) {
        if (virtualThreads) {
            log.info("[AsyncConfig - mailExecutor()] - Virtual Thread 모드");
            return new VirtualThreadTaskExecutor("MailVT-");
        }
        log.info("[AsyncConfig - mailExecutor()] - 플랫폼 스레드 모드 (core 15 / max 50 / queue 100)");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(15);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("MailExecutor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }

    // 뱃지 처리 전용 — 내부 DB 작업이라 동시 접근을 코어 3개로 묶는다.
    // 이 제한이 곧 백프레셔다. HikariCP 10개를 요청 스레드와 나눠 쓰므로 무제한이면 안됨
    // 메일 풀과 분리해 상호 영향을 차단
    @Bean(name = "BadgeExecutor")
    public Executor badgeExecutor(@Value(ASYNC_VT) boolean virtualThreads) {
        if (virtualThreads) {
            log.info("[AsyncConfig - badgeExecutor()] - Virtual Thread 모드 (상한 없음)");
            return new VirtualThreadTaskExecutor("BadgeVT-");
        }
        log.info("[AsyncConfig - badgeExecutor()] - 플랫폼 스레드 모드 (core 3 / max 10 / queue 1000)");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        // 50 → 1000: 초반 트랜션트에서 회차 12분을 RejectedExecution 으로 날리지 않기 위한 보험.
        // ThreadPoolTaskExecutor 는 코어가 차면 큐를 먼저 채우고 큐가 가득 차야 max 로 늘어나므로,
        // 큐가 얕으면 "코어 3개로 버티다 갑자기 거절"하는 이상한 상황
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("BadgeExecutor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }
}
