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

        log.info ("[DistributedLockAspect] - 락 획득 요청 발생, key: {}, wait/leaseTime: {}s, {}s ", distributedLock.key(), distributedLock.waitTime(), distributedLock.leaseTime());
        String lockKey = parseKey(joinPoint, distributedLock.key());
        long waitTime = distributedLock.waitTime();
        long leaseTime = distributedLock.leaseTime();

        RLock lock = redissonClient.getLock(lockKey);
        Boolean isLocked = false;

        try{
            isLocked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!isLocked){ // 여기서 false
                log.warn("[DistributedLockAspect] - 락 획득 실패, {}", lockKey);
                throw new GeneralException(ErrorCode.CANT_GET_LOCK);
            }
            log.info("[DistributedLockAspect] - 락 획득 성공, {}", lockKey);
            return joinPoint.proceed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[DistributedLockAspect] - 락 획득 실패, {}", lockKey);
            throw new GeneralException(ErrorCode.CANT_GET_LOCK);
        }
        finally {
            if (isLocked && lock.isHeldByCurrentThread()){ // 현 스레드가 락 보유 중인 지 확인하는 메소드.
                try{
                    lock.unlock();
                    log.info("[DistributedLockAspect] - 락 반납 완료, {}", lockKey);
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
