package DummyTalk.DummyTalk_BE.domain.controller;

import DummyTalk.DummyTalk_BE.domain.dto.admin.AdminRespDTO;
import DummyTalk.DummyTalk_BE.domain.entity.Quiz;
import DummyTalk.DummyTalk_BE.domain.service.admin.AdminService;
import DummyTalk.DummyTalk_BE.domain.service.dummy.DummyService;
import DummyTalk.DummyTalk_BE.domain.service.member.MemberService;
import DummyTalk.DummyTalk_BE.global.apiResponse.APIResponse;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.SuccessCode;
import DummyTalk.DummyTalk_BE.global.security.userDetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DummyService dummyService;
    private final MemberService memberService;
    private final AdminService adminService;

    // ===================== 대시보드 (DailySettlement) =====================

    // 특정 날짜 정산 단건 조회 — AdminTask 가 전날치를 00:30 에 저장하므로 오늘 날짜는 없음
    @GetMapping("/dashboard/daily")
    public APIResponse<AdminRespDTO.DailySettlementRespDTO> getDailySettlement(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return APIResponse.onSuccess(
                adminService.getDailySettlement(userDetails.getMember().getId(), date),
                SuccessCode.GET_SETTLEMENT_SUCCESS);
    }

    // 기간별 정산 목록 조회
    @GetMapping("/dashboard/range")
    public APIResponse<List<Object>> getSettlementRange(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return null;
    }

    // 최근 N일 정산 목록 (기본값 7일)
    @GetMapping("/dashboard/latest")
    public APIResponse<List<Object>> getLatestSettlements(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "7") Integer days) {
        return null;
    }

    // ===================== 공지사항 관리 =====================

    // 전체 목록 조회 — 비공개(임시저장) 포함
    @GetMapping("/notices")
    public APIResponse<List<Object>> getAllNotices(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return null;
    }

    // 공지사항 상세 조회
    @GetMapping("/notices/{id}")
    public APIResponse<Object> getNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        return null;
    }

    // 공지사항 작성
    @PostMapping("/notices")
    public APIResponse<Object> createNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Object dto) {
        return null;
    }

    // 공지사항 수정
    @PatchMapping("/notices/{id}")
    public APIResponse<Object> updateNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Object dto) {
        return null;
    }

    // 공지사항 삭제 (soft delete)
    @DeleteMapping("/notices/{id}")
    public APIResponse<Boolean> deleteNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        return null;
    }

    // 공지사항 공개/비공개 토글
    @PatchMapping("/notices/{id}/publish")
    public APIResponse<Boolean> toggleNoticePublish(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        return null;
    }

    // ===================== 기존 Admin 기능 이관 =====================

    // 퀴즈 오픈 — DummyService.openQuiz() 에서 ADMIN 체크 포함
    @PostMapping("/quiz/open")
    public APIResponse<Quiz> openQuiz(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "open-time") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        return APIResponse.onSuccess(
                adminService.openQuiz(userDetails.getMember().getId(), date),
                SuccessCode.OPEN_QUIZ_ADMIN_SUCCESS);
    }

    // 구독 승인 — MemberService.approveSubscription() 에서 ADMIN 체크 포함
    @PatchMapping("/members/subscribe")
    public APIResponse<Boolean> approveSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String email) {
        return APIResponse.onSuccess(
                memberService.approveSubscription(userDetails.getMember().getId(), email),
                SuccessCode.APPROVE_SUBSCRIBE_SUCCESS);
    }

    @GetMapping("/check-quiz")
    public APIResponse<Object> checkQuiz (@AuthenticationPrincipal CustomUserDetails userDetails){
        return APIResponse.onSuccess(adminService.checkQuiz(userDetails.getMember().getId()), SuccessCode.CHECK_QUIZ_SUCCESS);
    }
}