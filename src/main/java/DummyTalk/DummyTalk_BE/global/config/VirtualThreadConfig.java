package DummyTalk.DummyTalk_BE.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

@Configuration
public class VirtualThreadConfig {

    /**
     * Tomcat 요청 처리 executor 를 Virtual Thread 로 교체 — V4 전용.
     *
     * 요청당 VT 1개를 새로 만들므로 스레드 풀도 TaskQueue 도 없다
     * (그래서 VT 모드에서는 tomcat_threads_* 메트릭이 전부 -1 로 나온다. 실측 확인).
     *
     * 전에는 이 @Bean 의 주석을 손으로 풀고 조여서 VT 를 켰다 껐다 했는데,
     * spring.threads.virtual.enabled 와 따로 놀아 "V3 인 줄 알았는데 VT 가 켜져 있던" 혼동이 났다.
     * 이제 프로퍼티 하나를 따라가므로 .env 의 VIRTUAL_THREADS 만 바꾸면 된다 (재배포 불필요).
     *
     * 이 빈이 바꾸는 것은 "요청 처리" 스레드다. 비동기(badge/mail)는 AsyncConfig 가
     * concurrency.async-virtual-threads 로 따로 잡는데, 그 기본값이 이 프로퍼티를 따라가므로
     * .env 의 VIRTUAL_THREADS 하나로 두 경로가 함께 전환된다.
     *
     * 예전: AsyncConfig 가 전 회차 VT 고정이라 V1~V3 도 비동기만은 VT.
     * - 그 상태로 V4 를 "VT 도입"이라 부르면 효과가 과소 보고 -> 상한 없는 VT 가 요청 스레드와 경합해 CP pending 을 왜곡
     * (SchedulerConfig 의 VT 스케줄러는 요청 경로 밖이라 그대로 고정)
     */
    @Bean
    @ConditionalOnProperty(name = "spring.threads.virtual.enabled", havingValue = "true")
    public TomcatProtocolHandlerCustomizer<?> virtualThreadCustomizer() {
        return protocolHandler ->
                protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}
