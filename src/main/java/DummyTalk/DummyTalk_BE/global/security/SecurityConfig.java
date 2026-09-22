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
                            // hasRole("ADMIN") 이 아니라 hasAuthority("ADMIN") 인 이유:
                            //   CustomUserDetails.getAuthorities() 가 SimpleGrantedAuthority(role.name()) 즉 "ADMIN" 을 만든다.
                            //   hasRole 은 여기에 "ROLE_" 를 자동으로 붙여 "ROLE_ADMIN" 을 찾으므로 영원히 매칭되지 않아
                            //   ADMIN 계정도 403(Spring Security 기본 응답, APIResponse 아님)을 받았다. (K6 부하테스트 API 에서 발견)
                            //   권한 문자열에 ROLE_ 접두사를 붙이는 방향(Spring 관례)도 가능하지만, JWT 의 auth/role 클레임 값이
                            //   함께 바뀌어 프론트가 role === "ADMIN" 으로 비교 중이면 깨지므로 영향 범위가 좁은 쪽을 택했다.
                            .requestMatchers("/actuator/**").hasAuthority("ADMIN") // 관리자만 허용
                            .requestMatchers("/api/admin/**").hasAuthority("ADMIN") // 관리자만 허용
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
