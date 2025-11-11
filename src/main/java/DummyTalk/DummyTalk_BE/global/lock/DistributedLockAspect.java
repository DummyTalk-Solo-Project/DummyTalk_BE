package DummyTalk.DummyTalk_BE.global.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ExpressionParser expressionParser = new SpelExpressionParser();


    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = parseKey(joinPoint, distributedLock.key());
        long waitTime = distributedLock.waitTime();
        long leaseTime = distributedLock.leaseTime();

        RLock lock = redissonClient.getLock(lockKey);
        Boolean isLocked = false;

        try{
            isLocked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!isLocked){
                log.warn("락 획득 실패 - {}", lockKey);
                throw new RuntimeException("락 획득 오류!");
            }
            log.info("락 획득 성공 - {}", lockKey);
            return joinPoint.proceed();

        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        finally {
            if (isLocked && lock.isHeldByCurrentThread()){

                try{
                    lock.unlock();
                    log.info("락 반납 성공 - {}", lockKey);
                }
                catch (Exception e){
                    log.warn ("락 반납 실패 -> 이미 해제된 락 or 불일치, {} / {}", lockKey, e);
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
