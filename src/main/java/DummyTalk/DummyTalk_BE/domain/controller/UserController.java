package DummyTalk.DummyTalk_BE.domain.controller;

import DummyTalk.DummyTalk_BE.domain.service.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "사용자 API", description = "사용자 관련 API 입니다")
public class UserController {

    private final UserService userService;

    @GetMapping ("email-verification")
    public ResponseEntity<Object> sendVerificationEmail (){
        return ResponseEntity.ok(null);
    }


    @PostMapping("/login")
    public ResponseEntity<Object> login (){
        userService.login();
        return ResponseEntity.ok(null);
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(){
        return ResponseEntity.ok(null);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Object> subscribe (){
        return ResponseEntity.ok(null);
    }

    @GetMapping("/mypage")
    public ResponseEntity<Object> mypage (){
        return ResponseEntity.ok(null);
    }

    @PatchMapping("/withdrawal")
    public ResponseEntity<Object> withdrawal (){
        return ResponseEntity.ok(null);
    }
}
