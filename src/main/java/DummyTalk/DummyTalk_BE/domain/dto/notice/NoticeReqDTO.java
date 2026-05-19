package DummyTalk.DummyTalk_BE.domain.dto.notice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class NoticeReqDTO {

    // 공지사항 작성 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateNoticeDTO {
        private String title;
        private String content;
        // null이면 기본값 false (비고정)
        private Boolean isPinned;
    }

    // 공지사항 수정 요청 — null 필드는 변경하지 않음 (부분 수정)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateNoticeDTO {
        private String title;
        private String content;
        private Boolean isPinned;
    }
}