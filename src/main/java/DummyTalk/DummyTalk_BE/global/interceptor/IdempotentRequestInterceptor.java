package DummyTalk.DummyTalk_BE.global.interceptor;

import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.GeneralException;
import DummyTalk.DummyTalk_BE.global.interceptor.annotation.IdempotentRequest;
import DummyTalk.DummyTalk_BE.global.security.userDetails.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 동일 사용자의 중복(따닥) 요청을 Service 계층 앞단에서 차단하는 인터셉터.
 * 위치:  @DistributedLock보다 뒤!
 *
 * 인터셉터 도입 이유
 *   - Filter = DispatcherServlet 앞 = HandlerMethod·어노테이션 감지 불가
 *   - Interceptor = HandlerMethod 접근 가능 + SecurityContextHolder 사용 가능
 *   - Tomcat Thread는 이미 할당된 상태 → Thread 절약 목적이 아닌, 빠른 거절로 비싼 DB I/O를 방지하는 게 나을 듯
 *
 * 대충의 흐름
 * 1. HandlerMapping이 매핑 = URL, Method의 정보 획득 가능
 * 2. 인터셉터 실행! = preHandle() -> handlerMethod로 위 정보 접근 가능
 * 3. 실질적인 접근 허용/거부
 *
 * Redis 키 구조: idempotent:<memberId>:<httpMethod>:<requestURI>
 * TTL = 5초: getDummy leaseTime 4초 보다 약간 길게 설정한 failsafe.
 * afterCompletion에서 즉시 삭제하므로 정상 흐름에서는 TTL 만료 전 제거
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentRequestInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final long IDEMPOTENT_TTL_SECONDS = 45L;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 1. HandlerMethod 확인 — 정적 리소스, 에러 핸들러 등은 즉시 통과
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            // DispatcherServlet이 매핑한 결과가 HandlerMethod가 아닌 경우 (ResourceHttpRequestHandler 등)
            return true;
        }

        // 2. 커스텀 어노테이션 @IdempotentRequest 체크
         IdempotentRequest annotation = handlerMethod.getMethodAnnotation(IdempotentRequest.class);
         if (annotation == null) {
             return true; // 어노테이션 없는 메서드는 즉시 통과
         }

        // 3. SecurityContextHolder에서 인증 정보 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return true;
        }
        Long memberId = userDetails.getMember().getId();


        // 4. Redis SETNX — 원자적 연산으로 Race Condition 없이 중복 요청 감지
        // - setIfAbsent = SET key value NX EX ttl
        String redisKey = buildRedisKey(memberId, request);
        Boolean isFirstRequest = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", IDEMPOTENT_TTL_SECONDS, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(isFirstRequest)) {
            log.warn("[IdempotentRequestInterceptor - preHandle()] - 중복 요청 감지, memberId: {}, key: {}", memberId, redisKey);
            throw new GeneralException(ErrorCode.DUPLICATE_REQUEST);
        }

        log.info("[IdempotentRequestInterceptor - preHandle()] - 요청 허용, memberId: {}, key: {}", memberId, redisKey);
        return true;
    }

    // 5. 요청 완료 후 Redis 키
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        // 6. HandlerMethod 확인 — preHandle과 동일
        if (!(handler instanceof HandlerMethod)) {
            return;
        }

        // 7. 인증 정보 확인 — preHandle에서 키를 생성한 경우만 삭제
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return;
        }
        Long memberId = userDetails.getMember().getId();

        // 8. Redis 키 삭제 — TTL 만료를 기다리지 않고 즉시 제거 (SETNX)
        String redisKey = buildRedisKey(memberId, request);
        redisTemplate.delete(redisKey);
        log.info("[IdempotentRequestInterceptor - afterCompletion()] - Redis 키 삭제 완료, key: {}", redisKey);
    }

    private String buildRedisKey(Long memberId, HttpServletRequest request) {
        return "idempotent:" + memberId + ":" + request.getMethod() + ":" + request.getRequestURI();
    }
}
