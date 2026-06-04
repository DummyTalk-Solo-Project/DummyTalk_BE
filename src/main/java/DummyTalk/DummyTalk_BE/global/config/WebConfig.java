package DummyTalk.DummyTalk_BE.global.config;

import DummyTalk.DummyTalk_BE.global.interceptor.IdempotentRequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final IdempotentRequestInterceptor idempotentRequestInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 뱃지 이미지 - EC2 마운트 경로 → URL /uploads/badges/~~
        registry.addResourceHandler("/uploads/badges/**")
                .addResourceLocations("file:/app/uploads/badges/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 일단 필요한 데만 인터셉터 등록
        // 추후 등록해도 되니깐
        registry.addInterceptor(idempotentRequestInterceptor)
                .addPathPatterns(
                        "/api/dummies/dummy",
                        "/api/dummies/quiz"
                );
    }
}