package DummyTalk.DummyTalk_BE.global.lock;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {
    String key();
    long waitTime() default 5; // 락 획득 대기 시간
    long leaseTime() default 5; // 락 최대 점유 시간. Ns 이후 바로 락 반환
}