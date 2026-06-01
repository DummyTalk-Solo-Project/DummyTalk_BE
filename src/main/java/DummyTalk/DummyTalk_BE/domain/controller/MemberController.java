package DummyTalk.DummyTalk_BE.domain.controller;

import DummyTalk.DummyTalk_BE.domain.dto.member.MemberReqDTO;
import DummyTalk.DummyTalk_BE.domain.dto.member.MemberRespDTO;
import DummyTalk.DummyTalk_BE.domain.service.member.MemberService;
import DummyTalk.DummyTalk_BE.global.apiResponse.APIResponse;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.SuccessCode;
import DummyTalk.DummyTalk_BE.global.security.userDetails.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping ("/email-verification")
    public APIResponse<Boolean> sendVerificationEmail (@RequestParam String email) {
        memberService.sendVerificationEmail(email);

        return APIResponse.onSuccess(true, SuccessCode.EMAIL_SEND_SUCCESS);
    }

    @PostMapping ("/verify")
    public APIResponse<Boolean> verifyEmail (@RequestBody MemberReqDTO.VerificationRequestDTO requestDTO) {
        memberService.verifyEmail(requestDTO);
        return APIResponse.onSuccess(true, SuccessCode.VALIDATE_SUCCESS);
    }

    @PostMapping("/sign-in")
    public APIResponse<Boolean> signIn (@RequestBody MemberReqDTO.SignInRequestDTO request){
        memberService.signIn(request);
        return APIResponse.onSuccess(true, SuccessCode.SIGN_IN_SUCCESS);
    }

    @PostMapping("/login")
    public APIResponse<MemberRespDTO.LoginSuccessDTO> login (@RequestBody MemberReqDTO.LoginRequestDTO dto, HttpServletResponse response){

        MemberRespDTO.MemberInfoDTO memberInfo = memberService.login(dto);

        // SameSite=None: FE(Vercel)·BE(EC2)가 다른 도메인 → 크로스 사이트 쿠키 전송 필요, Secure 필수
        ResponseCookie cookie = ResponseCookie.from("refreshToken", memberInfo.getJwt().getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7일
                .sameSite("None")
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        MemberRespDTO.LoginSuccessDTO respDTO = MemberRespDTO.LoginSuccessDTO.builder()
                .memberName(memberInfo.getUsername())
                .isSuccess(true)
                .accessToken(memberInfo.getJwt().getAccessToken())
                .needPasswordChange(memberInfo.getNeedPasswordChange())
                .build();

        // RFC 6750 표준: "Bearer <token>" (콜론 없음)
        response.addHeader("Authorization", "Bearer " + respDTO.getAccessToken());

        return APIResponse.onSuccess(respDTO, SuccessCode.LOGIN_SUCCESS);
    }

    @PostMapping("/logout")
    public APIResponse<Boolean> logout(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            HttpServletRequest request){

        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            memberService.logout(bearerToken.substring(7).trim(), customUserDetails.getMember().getId());
        }
        return APIResponse.onSuccess(true,  SuccessCode.LOGOUT_SUCCESS);
    }

//    @PostMapping("/subscribe")
    public APIResponse<Boolean> subscribe (@AuthenticationPrincipal CustomUserDetails userDetails){
        // 구독 신청
        return APIResponse.onSuccess(memberService.subscribe(userDetails.getMember().getId()), SuccessCode.SUBSCRIBE_SUCCESS);
    }

    // 구독 승인 후 최초 홈 진입 시 팝업 여부 확인 — true면 팝업 노출, 1회 소비 후 false -> 1회성 팝업용 설계가 필요!
//    @GetMapping("/subscription-popup")
    public APIResponse<Boolean> checkSubscriptionPopup(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return APIResponse.onSuccess(memberService.checkSubscriptionPopup(userDetails.getMember().getId()), SuccessCode.SUBSCRIPTION_POPUP_SUCCESS);
    }

    @GetMapping("/my-page")
    public APIResponse<MemberRespDTO.GetMemberResponseDTO> getMyPage(@AuthenticationPrincipal CustomUserDetails userDetails){
        return APIResponse.onSuccess(memberService.getMyData(userDetails.getMember().getId()), SuccessCode.GET_INFO_SUCCESS);
    }

    @PatchMapping("/withdrawal")
    public APIResponse<Boolean> withdrawal (@AuthenticationPrincipal CustomUserDetails userDetails){
        memberService.withdraw(userDetails.getMember().getId());
        return APIResponse.onSuccess(true, SuccessCode.WITHDRAWN_SUCCESS);
    }

    // 탈퇴 후 2주 이내 재로그인 시 복구 요청 — 프론트에서 MEMBER_WITHDRAWN_RESTORABLE 수신 후 호출
    @PatchMapping("/restore")
    public APIResponse<MemberRespDTO.LoginSuccessDTO> restore(@RequestBody MemberReqDTO.LoginRequestDTO dto, HttpServletResponse response) {

        MemberRespDTO.MemberInfoDTO memberInfo = memberService.restoreAccount(dto);

        // SameSite=None: 크로스 도메인 쿠키 전송 필요, Secure 필수
        ResponseCookie cookie = ResponseCookie.from("refreshToken", memberInfo.getJwt().getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("None")
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.addHeader("Authorization", "Bearer " + memberInfo.getJwt().getAccessToken());

        MemberRespDTO.LoginSuccessDTO respDTO = MemberRespDTO.LoginSuccessDTO.builder()
                .memberName(memberInfo.getUsername())
                .isSuccess(true)
                .accessToken(memberInfo.getJwt().getAccessToken())
                .build();

        return APIResponse.onSuccess(respDTO, SuccessCode.LOGIN_SUCCESS);
    }

    @GetMapping("/find-email")
    public APIResponse<String> findEmail(@RequestParam String email) {
        return APIResponse.onSuccess(memberService.findEmail(email), SuccessCode.FIND_EMAIL_SUCCESS);
    }

    @PatchMapping("/change-password")
    public APIResponse<Boolean> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody MemberReqDTO.ChangePasswordRequestDTO dto) {
        memberService.changePassword(userDetails.getMember().getId(), dto);
        return APIResponse.onSuccess(true, SuccessCode.PASSWORD_CHANGE_SUCCESS);
    }

    @PostMapping("/reset-password")
    public APIResponse<Boolean> resetPassword(@RequestParam String email) {
        memberService.resetPassword(email);
        return APIResponse.onSuccess(true, SuccessCode.PASSWORD_RESET_SUCCESS);
    }

    @GetMapping("/check-email")
    public APIResponse<Boolean> checkEmailDuplicate(@RequestParam String email) {
        return APIResponse.onSuccess(memberService.checkEmailDuplicate(email), SuccessCode.CHECK_EMAIL_SUCCESS);
    }

    @GetMapping("/request-code")
    public APIResponse<Boolean> requestVerificationCode(@RequestParam String email) {
        memberService.requestVerificationCode(email);
        return APIResponse.onSuccess(true, SuccessCode.EMAIL_SEND_SUCCESS);
    }
}
