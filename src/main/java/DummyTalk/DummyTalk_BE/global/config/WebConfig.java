package DummyTalk.DummyTalk_BE.global.config;

import DummyTalk.DummyTalk_BE.global.interceptor.IdempotentRequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final IdempotentRequestInterceptor idempotentRequestInterceptor;

    /**
     * K6 동시성 전략 비교를 위한 인터셉터 ON/OFF 플래그
     * application.yml: concurrency.interceptor-enabled
     *
     * true  (default) = V3 테스트: 인터셉터 등록, 따닥 요청 429 차단
     * false           = V1/V2 테스트: 인터셉터 미등록, @IdempotentRequest 어노테이션은 있지만 효과 없음
     */
    @Value("${concurrency.interceptor-enabled:true}")
    private boolean interceptorEnabled;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 뱃지 이미지 - EC2 마운트 경로 → URL /uploads/badges/~~
        registry.addResourceHandler("/uploads/badges/**")
                .addResourceLocations("file:/app/uploads/badges/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (!interceptorEnabled) {
            return; // V1/V2 테스트 시: 인터셉터 비활성화
        }
        // V3 테스트 시: 동시성 보호가 필요한 경로만 등록
        registry.addInterceptor(idempotentRequestInterceptor)
                .addPathPatterns(
                        "/api/dummies/dummy",
                        "/api/dummies/quiz"
                );
    }
}