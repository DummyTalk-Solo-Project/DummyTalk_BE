package DummyTalk.DummyTalk_BE.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 뱃지 이미지 정적 리소스 서빙 설정.
 * EC2 /home/ubuntu/data/badges/ → 컨테이너 /app/uploads/badges/ (docker-compose bind mount)
 * 요청 URL: /uploads/badges/{filename}
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 뱃지 이미지: EC2 마운트 경로 → URL /uploads/badges/**
        registry.addResourceHandler("/uploads/badges/**")
                .addResourceLocations("file:/app/uploads/badges/");
    }
}