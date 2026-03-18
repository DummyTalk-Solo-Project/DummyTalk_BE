package DummyTalk.DummyTalk_BE.domain.controller;

import DummyTalk.DummyTalk_BE.domain.dto.member.MemberRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.member.MemberResponseDTO;
import DummyTalk.DummyTalk_BE.domain.service.member.MemberService;
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
public class MemberController {

    private final MemberService memberService;

    @GetMapping ("/email-verification")
    public APIResponse<Boolean> sendVerificationEmail (@RequestParam String email) {
        memberService.sendVerificationEmail(email);

        return APIResponse.onSuccess(true, SuccessCode.EMAIL_SEND_SUCCESS);
    }

    @PostMapping ("/verify")
    public APIResponse<Boolean> verifyEmail (@RequestBody MemberRequestDTO.VerificationRequestDTO requestDTO) {
        memberService.verifyEmail(requestDTO);
        return APIResponse.onSuccess(true, SuccessCode.VALIDATE_SUCCESS);
    }

    @PostMapping("/sign-in")
    public APIResponse<Boolean> signIn (@RequestBody MemberRequestDTO.SignInRequestDTO request){
        memberService.signIn(request);
        return APIResponse.onSuccess(true, SuccessCode.SIGN_IN_SUCCESS);
    }

    @PostMapping("/login")
    public APIResponse<Boolean> login (@RequestBody MemberRequestDTO.LoginRequestDTO requestDTO, HttpServletResponse response){
        MemberResponseDTO.LoginSuccessDTO responseDTO = memberService.login(requestDTO);

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
    public APIResponse<List<MemberResponseDTO.GetUserResponseDTO>> mypage (@AuthenticationPrincipal CustomUserDetails userDetails){
        return APIResponse.onSuccess(memberService.getAllData(), SuccessCode.GET_INFO_SUCCESS);
    }

    @PatchMapping("/withdrawal")
    public APIResponse<Boolean> withdrawal (@AuthenticationPrincipal CustomUserDetails userDetails){
        memberService.withdraw(userDetails.getMember().getEmail());
        return APIResponse.onSuccess(true, SuccessCode.WITHDRAWN_SUCCESS);
    }

    // mypage로 변경
    /*@GetMapping("/get-all-data")
    public List<UserResponseDTO.GetUserResponseDTO> getAllData () {
        return userService.getAllData();
    }*/
}
