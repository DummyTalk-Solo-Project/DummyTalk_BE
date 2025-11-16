package DummyTalk.DummyTalk_BE.global.lock;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {
    String key();
    long waitTime() default 5;
    long leaseTime() default 10;
}