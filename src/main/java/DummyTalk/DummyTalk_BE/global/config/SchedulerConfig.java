package DummyTalk.DummyTalk_BE.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

@Configuration
public class SchedulerConfig {

    /**
     * Spring 6.1+ VT 지원 스케줄러:
     * - VirtualThreadTaskScheduler 는 별도 클래스로 존재하지 않음
     * - SimpleAsyncTaskScheduler.setVirtualThreads(true) 로 VT 스케줄러 구성
     *
     * 적용 대상:
     * - @Scheduled Method (MemberScheduler, AdminScheduler, BatchScheduler) → VT 위에서 실행
     * - AdminService.taskScheduler.schedule() 퀴즈 예약 → VT 위에서 실행
     *
     * Spring Boot 자동설정 = 내부적으로 TaskScheduler 구현 Bean을 하나 더 등록!
     * @Primary를 추가로 현 구련 Bean을 명시적으로 우선순위로 지정
     */
    @Bean
    @Primary
    public TaskScheduler taskScheduler() {
        SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
        scheduler.setVirtualThreads(true);
        scheduler.setThreadNamePrefix("SchedulerVT-");
        return scheduler;
    }
}
