package DummyTalk.DummyTalk_BE.domain.controller;

import DummyTalk.DummyTalk_BE.domain.dto.admin.AdminRespDTO;
import DummyTalk.DummyTalk_BE.domain.dto.notice.NoticeReqDTO;
import DummyTalk.DummyTalk_BE.domain.dto.notice.NoticeRespDTO;
import DummyTalk.DummyTalk_BE.domain.entity.Quiz;
import DummyTalk.DummyTalk_BE.domain.service.admin.AdminService;
import DummyTalk.DummyTalk_BE.domain.service.member.MemberService;
import DummyTalk.DummyTalk_BE.domain.service.notice.NoticeService;
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

    private final MemberService memberService;
    private final AdminService adminService;
    private final NoticeService noticeService;

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
    public APIResponse<List<AdminRespDTO.DailySettlementRespDTO>> getSettlementRange(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return APIResponse.onSuccess(
                adminService.getSettlementRange(userDetails.getMember().getId(), from, to),
                SuccessCode.GET_SETTLEMENT_SUCCESS);
    }

    // 최근 N일 정산 목록 (기본값 7일)
    @GetMapping("/dashboard/latest")
    public APIResponse<List<AdminRespDTO.DailySettlementRespDTO>> getLatestSettlements(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "7") Integer days) {
        return APIResponse.onSuccess(
                adminService.getLatestSettlements(userDetails.getMember().getId(), days),
                SuccessCode.GET_SETTLEMENT_SUCCESS);
    }

    // ===================== 공지사항 관리 =====================

    // 전체 목록 조회 — 비공개(임시저장) 포함
    @GetMapping("/notices")
    public APIResponse<List<NoticeRespDTO.NoticeListItemDTO>> getAllNotices(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page) {
        return APIResponse.onSuccess(
                noticeService.getAllNotices(userDetails.getMember().getId(), page),
                SuccessCode.GET_NOTICE_SUCCESS);
    }

    // 공지사항 상세 조회 — 비공개도 조회 가능
    @GetMapping("/notices/{id}")
    public APIResponse<NoticeRespDTO.NoticeDetailDTO> getNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        return APIResponse.onSuccess(
                noticeService.getNotice(userDetails.getMember().getId(), id),
                SuccessCode.GET_NOTICE_SUCCESS);
    }

    // 공지사항 작성
    @PostMapping("/notices")
    public APIResponse<NoticeRespDTO.NoticeDetailDTO> createNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody NoticeReqDTO.CreateNoticeDTO dto) {
        return APIResponse.onSuccess(
                noticeService.createNotice(userDetails.getMember().getId(), dto),
                SuccessCode.CREATE_NOTICE_SUCCESS);
    }

    // 공지사항 수정 (부분 수정 — null 필드 제외)
    @PatchMapping("/notices/{id}")
    public APIResponse<NoticeRespDTO.NoticeDetailDTO> updateNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody NoticeReqDTO.UpdateNoticeDTO dto) {
        return APIResponse.onSuccess(
                noticeService.updateNotice(userDetails.getMember().getId(), id, dto),
                SuccessCode.UPDATE_NOTICE_SUCCESS);
    }

    // 공지사항 삭제 (soft delete)
    @DeleteMapping("/notices/{id}")
    public APIResponse<Boolean> deleteNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        return APIResponse.onSuccess(
                noticeService.deleteNotice(userDetails.getMember().getId(), id),
                SuccessCode.DELETE_NOTICE_SUCCESS);
    }

    // 공지사항 공개/비공개 토글 — 변경 후 현재 isPublished 반환
    @PatchMapping("/notices/{id}/publish")
    public APIResponse<Boolean> toggleNoticePublish(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        return APIResponse.onSuccess(
                noticeService.toggleNoticePublish(userDetails.getMember().getId(), id),
                SuccessCode.TOGGLE_NOTICE_PUBLISH_SUCCESS);
    }

    // 퀴즈 오픈 — AdminService.openQuiz() 에서 ADMIN 체크 포함
    @PostMapping("/quiz/open")
    public APIResponse<Quiz> openQuiz(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "open-time") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        return APIResponse.onSuccess(
                adminService.openQuiz(userDetails.getMember().getId(), date),
                SuccessCode.OPEN_QUIZ_ADMIN_SUCCESS);
    }

    // 구독 승인 — MemberService.approveSubscription() 에서 ADMIN 체크 포함
//    @PatchMapping("/members/subscribe")
    public APIResponse<Boolean> approveSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String email) {
        return APIResponse.onSuccess(
                memberService.approveSubscription(userDetails.getMember().getId(), email),
                SuccessCode.APPROVE_SUBSCRIBE_SUCCESS);
    }

    // 퀴즈 스케줄러 상태 확인
    @GetMapping("/check-quiz")
    public APIResponse<Object> checkQuiz(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return APIResponse.onSuccess(adminService.checkQuiz(userDetails.getMember().getId()), SuccessCode.CHECK_QUIZ_SUCCESS);
    }
}