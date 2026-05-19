package DummyTalk.DummyTalk_BE.domain.controller;

import DummyTalk.DummyTalk_BE.domain.dto.notice.NoticeRespDTO;
import DummyTalk.DummyTalk_BE.domain.service.notice.NoticeService;
import DummyTalk.DummyTalk_BE.global.apiResponse.APIResponse;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.SuccessCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    // 공개된 공지사항 목록 — isPublished=true 인 것만 반환, isPinned 우선 정렬
    @GetMapping
    public APIResponse<List<NoticeRespDTO.NoticeListItemDTO>> getPublishedNotices(
            @RequestParam(defaultValue = "0") int page) {
        return APIResponse.onSuccess(
                noticeService.getPublishedNotices(page),
                SuccessCode.GET_NOTICE_SUCCESS);
    }

    // 공개된 공지사항 상세 — isPublished=false 이면 NOTICE4002 반환
    @GetMapping("/{id}")
    public APIResponse<NoticeRespDTO.NoticeDetailDTO> getPublishedNotice(@PathVariable Long id) {
        return APIResponse.onSuccess(
                noticeService.getPublishedNotice(id),
                SuccessCode.GET_NOTICE_SUCCESS);
    }
}