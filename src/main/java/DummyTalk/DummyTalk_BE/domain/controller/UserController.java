package DummyTalk.DummyTalk_BE.domain.controller;

import DummyTalk.DummyTalk_BE.domain.dto.user.UserRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.user.UserResponseDTO;
import DummyTalk.DummyTalk_BE.domain.entity.user.User;
import DummyTalk.DummyTalk_BE.domain.service.user.UserService;
import DummyTalk.DummyTalk_BE.global.security.userDetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "사용자 API", description = "사용자 관련 API 입니다")
public class UserController {

    private final UserService userService;

    @GetMapping ("/email-verification")
    public ResponseEntity<Object> sendVerificationEmail (@RequestParam String email) {
        userService.sendVerificationEmail(email);
        return ResponseEntity.ok("send OK");
    }

    @PostMapping ("/verify")
    public ResponseEntity<Object> verifyEmail (@RequestBody UserRequestDTO.VerificationRequestDTO requestDTO) {
        userService.verifyEmail(requestDTO);
        return  ResponseEntity.ok("verify OK!");
    }

    @PostMapping("/sign-in")
    public ResponseEntity<Object> signIn (@RequestBody UserRequestDTO.SignInRequestDTO request){
        userService.signIn(request);
        return ResponseEntity.ok("sign In OK!");
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login (@RequestBody UserRequestDTO.LoginRequestDTO requestDTO, HttpServletResponse response){
        UserResponseDTO.LoginSuccessDTO responseDTO = userService.login(requestDTO);

        response.addHeader("Authorization", "Bearer: " + responseDTO.getAccessToken());

        // JWT 발급 메소드 호출은 어떻게?
        return ResponseEntity.ok("LOGIN OK, Welcome " + responseDTO.getUsername() );
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(){
        return ResponseEntity.ok(null);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Object> subscribe (){
        return ResponseEntity.ok(null);
    }

    @GetMapping("/my-page")
    public ResponseEntity<Object> mypage (@AuthenticationPrincipal CustomUserDetails userDetails){
        return ResponseEntity.ok(userDetails.getUser());
    }

    @PatchMapping("/withdrawal")
    public ResponseEntity<Object> withdrawal (){
        return ResponseEntity.ok(null);
    }
}
