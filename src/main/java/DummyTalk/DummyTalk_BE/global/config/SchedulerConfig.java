package DummyTalk.DummyTalk_BE.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

 import org.springframework.context.annotation.Primary;
 import org.springframework.scheduling.TaskScheduler;
 import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

@Configuration
public class SchedulerConfig {

    // Stage 1~3: ThreadPoolTaskScheduler (PT 기반)
//    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("[ThreadPoolTaskScheduler] - ");
        scheduler.initialize();
        return scheduler;
    }

     @Bean
     @Primary
     public TaskScheduler VTTaskScheduler() {
         SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
         scheduler.setVirtualThreads(true);
         scheduler.setThreadNamePrefix("SchedulerVT-");
         return scheduler;
     }
}
