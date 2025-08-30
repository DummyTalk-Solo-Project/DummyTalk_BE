package DummyTalk.DummyTalk_BE.domain.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/users")
@Tag(name = "사용자 API", description = "사용자 관련 API 입니다")
public class UserController {


    @GetMapping ("email-verification")
    public ResponseEntity<Object> sendVerificationEmail (){
        return ResponseEntity.ok(null);
    }
}
