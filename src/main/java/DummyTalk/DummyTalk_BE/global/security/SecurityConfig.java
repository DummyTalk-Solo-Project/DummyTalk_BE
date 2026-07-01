package DummyTalk.DummyTalk_BE.global.security;

import DummyTalk.DummyTalk_BE.global.security.filter.JWTAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig{

    private final JWTAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> {
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests((auth) -> {
                    auth
                            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                            .requestMatchers("/actuator/prometheus", "/actuator/health").permitAll() // 접근 허용 + Grafana는 잠그기
                            .requestMatchers("/actuator/**").hasRole("ADMIN") // 관리자만 허용
                            .requestMatchers("/api/admin/**").hasRole("ADMIN") // 관리자만 허용
                            .requestMatchers("/api/alarms/**").authenticated()
                            .requestMatchers("/api/missions/**").authenticated()
                            .requestMatchers("/api/members/me/**").authenticated()
                            .anyRequest().permitAll();
                });
        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "https://dummytalk.vercel.app",
                "http://localhost:3000",
                "http://localhost:5173"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization")); // AT 갱신 응답의 Authorization 헤더를 FE JS에서 읽으려면 명시적 expose 필요
        config.setAllowCredentials(true); // llowedOrigins에 * 사용 불가
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
