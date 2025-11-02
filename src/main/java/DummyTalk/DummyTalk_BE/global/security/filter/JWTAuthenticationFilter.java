package DummyTalk.DummyTalk_BE.global.security.filter;

import DummyTalk.DummyTalk_BE.global.security.jwt.JWTProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private final JWTProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String accessToken = resolveToken(request);
        log.info("Extracted Token: {}", accessToken);

        if(accessToken != null && jwtProvider.validateToken(accessToken)){
            Authentication authentication = jwtProvider.getAuthentication(accessToken);
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("인증 정보 SecurityContextHolder에 저장 완료. User: {}", authentication.getName());
            } else {
                log.info("유효하지 않은 토큰입니다. 인증 정보를 저장하지 않습니다.");
            }
        } else {
            log.info("요청 헤더에 유효한 JWT 토큰이 없습니다, 비로그인 사용자일 수   있습니다.");
        }
//        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        log.info("request! {}", bearerToken);
        if (bearerToken != null && bearerToken.startsWith("Bearer: ")) {
            String substring = bearerToken.substring(8);
            log.info("substring: {}", substring);
            return substring;
        }
        return null;
    }
}