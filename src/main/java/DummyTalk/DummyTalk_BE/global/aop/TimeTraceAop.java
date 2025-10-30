package DummyTalk.DummyTalk_BE.global.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class TimeTraceAop {

    @Around("execution(* DummyTalk.DummyTalk_BE.domain.service.user.impl..*(..))")
    public Object traceExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long end = System.currentTimeMillis();
            long duration = end - start;
            log.info("[START] Method: {} | StartTime: {}ms",
                    joinPoint.getSignature(), start);
            log.info("[END]   Method: {} | EndTime: {}ms | ExecutionTime: {}ms",
                    joinPoint.getSignature(), end, duration);
        }
    }
}