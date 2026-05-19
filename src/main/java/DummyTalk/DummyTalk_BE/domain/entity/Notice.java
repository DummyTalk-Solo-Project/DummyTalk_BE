package DummyTalk.DummyTalk_BE.domain.entity;

import DummyTalk.DummyTalk_BE.domain.entity.common.CommonEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Notice extends CommonEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    // 본문은 길이 제한 없이 TEXT 타입으로 저장
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 상단 고정 여부 — 목록 최상단 노출용
    @Column(nullable = false)
    @Builder.Default
    private Boolean isPinned = false;

    // 공개 여부 — false: 임시저장, true: 사용자에게 공개
    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    // 작성한 관리자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Member author;

    // 제목·본문·고정 여부 부분 수정 — null 필드는 변경하지 않음
    public void update(String title, String content, Boolean isPinned) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (isPinned != null) this.isPinned = isPinned;
    }

    // 공개/비공개 토글
    public void togglePublish() {
        this.isPublished = !this.isPublished;
    }

    // softDelete() 위임 — CommonEntity.softDelete()는 protected
    public void delete() {
        softDelete();
    }
}