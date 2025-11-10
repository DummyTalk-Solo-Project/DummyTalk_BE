package DummyTalk.DummyTalk_BE.domain.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public void force500Error() {
        throw new RuntimeException("강제 500 에러 테스트");
    }
}