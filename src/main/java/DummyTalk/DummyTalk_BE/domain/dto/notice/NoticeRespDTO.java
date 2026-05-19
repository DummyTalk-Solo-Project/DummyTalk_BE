package DummyTalk.DummyTalk_BE.domain.dto.notice;

import DummyTalk.DummyTalk_BE.domain.entity.Notice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class NoticeRespDTO {

    // 목록 아이템 — 본문 제외, 공개 여부 포함 (Admin 목록에도 동일하게 사용)
    @Getter
    @Builder
    public static class NoticeListItemDTO {
        private Long id;
        private String title;
        private Boolean isPinned;
        private Boolean isPublished;
        private String authorName;
        private LocalDateTime createdAt;

        public static NoticeListItemDTO from(Notice n) {
            return NoticeListItemDTO.builder()
                    .id(n.getId())
                    .title(n.getTitle())
                    .isPinned(n.getIsPinned())
                    .isPublished(n.getIsPublished())
                    .authorName(n.getAuthor() != null ? n.getAuthor().getMemberName() : null)
                    .createdAt(n.getCreatedAt())
                    .build();
        }
    }

    // 상세 — 본문 포함
    @Getter
    @Builder
    public static class NoticeDetailDTO {
        private Long id;
        private String title;
        private String content;
        private Boolean isPinned;
        private Boolean isPublished;
        private String authorName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static NoticeDetailDTO from(Notice n) {
            return NoticeDetailDTO.builder()
                    .id(n.getId())
                    .title(n.getTitle())
                    .content(n.getContent())
                    .isPinned(n.getIsPinned())
                    .isPublished(n.getIsPublished())
                    .authorName(n.getAuthor() != null ? n.getAuthor().getMemberName() : null)
                    .createdAt(n.getCreatedAt())
                    .updatedAt(n.getUpdatedAt())
                    .build();
        }
    }
}