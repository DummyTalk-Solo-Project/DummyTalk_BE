package DummyTalk.DummyTalk_BE.global.config;

import org.springframework.context.annotation.Configuration;


@Configuration
public class VirtualThreadConfig {
    /*
    *  [TODO 구현 순서]
   Step 1. Tomcat Virtual Thread Executor 설정
   Step 2. Async Executor (BadgeExecutor, mailExecutor) VirtualThreadTaskExecutor 교체
   Step 3. TaskScheduler VirtualThreadTaskScheduler로 교체
   Step 4. -Djdk.tracePinnedThreads=full 으로 Pinning 진단
   Step 5. K6 Before/After 측정 및 포트폴리오 문서화
    * */

}
