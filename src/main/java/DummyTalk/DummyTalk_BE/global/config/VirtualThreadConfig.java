package DummyTalk.DummyTalk_BE.global.config;

import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

@Configuration
public class VirtualThreadConfig {


    /**
     * [Stage 4 전용 — VT 활성화 시 아래 @Bean 주석 해제]
     * 변경: 요청당 VT 1개 생성, Pool/Queue 없음 → Thread 소진으로 인한 Rejection 불가
     * CP 튜닝(Stage 4) 동시 적용 필요!
     */
     @Bean
     public TomcatProtocolHandlerCustomizer<?> virtualThreadCustomizer() {
         return protocolHandler ->
             protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
     }
}
