package DummyTalk.DummyTalk_BE.global.lock;

import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@Aspect
@Order(1)
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ExpressionParser expressionParser = new SpelExpressionParser();


    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {

        /*
         * 아래 3줄(획득 요청 / 획득 성공 / 반납 완료)은 요청당 1건씩이라 info 로 두면 부하 회차를 오염시킨다.
         * V2(분산락) 200rps 실측: 로그가 초당 627건으로 V1(146건)의 4.3배였고, CPU(system) 이 63.8% → 70.2%,
         * load 1m 이 3.99 → 5.30 으로 올랐다. "분산락이 비싸다" 가 락 때문인지 로깅 때문인지 가릴 수 없게 된다.
         * 동작 추적이 필요하면 LOG_LEVEL_APP=DEBUG 로 다시 켠다.
         */
        log.debug("[DistributedLockAspect] - 락 획득 요청 발생, key: {}, wait/leaseTime: {}s, {}s ", distributedLock.key(), distributedLock.waitTime(), distributedLock.leaseTime());
        String lockKey = parseKey(joinPoint, distributedLock.key());
        long waitTime = distributedLock.waitTime();
        long leaseTime = distributedLock.leaseTime();

        RLock lock = redissonClient.getLock(lockKey);
        Boolean isLocked = false;

        try{
            isLocked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!isLocked){ // 여기서 false
                // 따닥 차단은 설계된 정상 동작이라 warn 이 아니라 debug. 폭주 시 거절 1건당 로그 1줄이 그대로 비용이 된다
                log.debug("[DistributedLockAspect] - 락 획득 실패, 사유: 잠김, {}", lockKey);
                throw new GeneralException(ErrorCode.CANT_GET_LOCK);
            }
            log.debug("[DistributedLockAspect] - 락 획득 성공, {}", lockKey);
            return joinPoint.proceed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[DistributedLockAspect] - 락 획득 실패, 사유: InterruptedException, {}", lockKey);
            throw new GeneralException(ErrorCode.CANT_GET_LOCK);
        }
        finally {
            if (isLocked && lock.isHeldByCurrentThread()){ // 현 스레드가 락 보유 중인 지 확인하는 메소드.
                try{
                    lock.unlock();
                    log.debug("[DistributedLockAspect] - 락 반납 완료, {}", lockKey);
                }
                catch (Exception e){
                    log.warn("[DistributedLockAspect] - 락 반납 실패 (이미 해제됨 or 불일치), {}, ", lockKey, e);
                }
            }
        }
    }


    // 실행 메서드 이름 기준 락 키 생성
    private String parseKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        EvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();

        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        return expressionParser.parseExpression(keyExpression).getValue(context, String.class);
    }
}
