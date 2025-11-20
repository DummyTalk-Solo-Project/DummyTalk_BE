package DummyTalk.DummyTalk_BE.domain.controller;

import DummyTalk.DummyTalk_BE.domain.dto.user.UserRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.user.UserResponseDTO;
import DummyTalk.DummyTalk_BE.domain.service.user.UserService;
import DummyTalk.DummyTalk_BE.global.apiResponse.APIResponse;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.SuccessCode;
import DummyTalk.DummyTalk_BE.global.security.userDetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "사용자 API", description = "사용자 관련 API 입니다")
public class UserController {

    private final UserService userService;

    @GetMapping ("/email-verification")
    public APIResponse<Boolean> sendVerificationEmail (@RequestParam String email) {
        userService.sendVerificationEmail(email);

        return APIResponse.onSuccess(true, SuccessCode.EMAIL_SEND_SUCCESS);
    }

    @PostMapping ("/verify")
    public APIResponse<Boolean> verifyEmail (@RequestBody UserRequestDTO.VerificationRequestDTO requestDTO) {
        userService.verifyEmail(requestDTO);
        return APIResponse.onSuccess(true, SuccessCode.VALIDATE_SUCCESS);
    }

    @PostMapping("/sign-in")
    public APIResponse<Boolean> signIn (@RequestBody UserRequestDTO.SignInRequestDTO request){
        userService.signIn(request);
        return APIResponse.onSuccess(true, SuccessCode.SIGN_IN_SUCCESS);
    }

    @PostMapping("/login")
    public APIResponse<Boolean> login (@RequestBody UserRequestDTO.LoginRequestDTO requestDTO, HttpServletResponse response){
        UserResponseDTO.LoginSuccessDTO responseDTO = userService.login(requestDTO);

        response.addHeader("Authorization", "Bearer: " + responseDTO.getAccessToken());

        // JWT 발급 메소드 호출은 어떻게?
        return APIResponse.onSuccess(true, SuccessCode.LOGIN_SUCCESS);
    }

    @PostMapping("/logout")
    public APIResponse<Boolean> logout(){
        // 토큰 강제 만료 로직 추가
        return APIResponse.onSuccess(true,  SuccessCode.LOGOUT_SUCCESS);
    }

    @PostMapping("/subscribe")
    public APIResponse<Boolean> subscribe (){
        // 구독 신청
        return APIResponse.onSuccess(true, SuccessCode.SUBSCRIBE_SUCCESS);
    }

    @GetMapping("/my-page")
    public APIResponse<List<UserResponseDTO.GetUserResponseDTO>> mypage (@AuthenticationPrincipal CustomUserDetails userDetails){
        return APIResponse.onSuccess(userService.getAllData(), SuccessCode.GET_INFO_SUCCESS);
    }

    @PatchMapping("/withdrawal")
    public APIResponse<Boolean> withdrawal (@AuthenticationPrincipal CustomUserDetails userDetails){
        userService.withdraw(userDetails.getUser().getEmail());
        return APIResponse.onSuccess(true, SuccessCode.WITHDRAWN_SUCCESS);
    }

    // mypage로 변경
    /*@GetMapping("/get-all-data")
    public List<UserResponseDTO.GetUserResponseDTO> getAllData () {
        return userService.getAllData();
    }*/
}
