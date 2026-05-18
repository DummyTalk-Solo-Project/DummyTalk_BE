package DummyTalk.DummyTalk_BE.domain.controller;

import DummyTalk.DummyTalk_BE.global.apiResponse.APIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    // 공개된 공지사항 목록 — isPublished=true 인 것만 반환
    @GetMapping
    public APIResponse<List<Object>> getPublishedNotices() {
        return null;
    }

    // 공개된 공지사항 상세 — isPublished=false 이면 404 처리 예정
    @GetMapping("/{id}")
    public APIResponse<Object> getPublishedNotice(@PathVariable Long id) {
        return null;
    }
}