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
}