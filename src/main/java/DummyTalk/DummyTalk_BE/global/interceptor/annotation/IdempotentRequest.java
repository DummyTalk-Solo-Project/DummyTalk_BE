package DummyTalk.DummyTalk_BE.global.interceptor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotentRequest {
    /*
    * 인터셉터 중복 요청 방지(따닥용 방지)를 위핸 커스텀 어노테이션
    * 적용 대상 (예정)
    * - DummyController.getDummy()
    *  - DummyController.solveQuiz()
    * */
}
