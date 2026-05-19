package DummyTalk.DummyTalk_BE.domain.repository.jpa;

import DummyTalk.DummyTalk_BE.domain.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 공개 목록 — isPublished=true, isDeleted=false. isPinned 고정 글 우선, 최신순 정렬
    Page<Notice> findByIsPublishedTrueAndIsDeletedFalseOrderByIsPinnedDescCreatedAtDesc(Pageable pageable);

    // 전체 목록 (Admin) — isDeleted=false. 임시저장 포함
    Page<Notice> findByIsDeletedFalseOrderByIsPinnedDescCreatedAtDesc(Pageable pageable);

    // 단건 조회 — 삭제된 공지사항은 조회 불가
    Optional<Notice> findByIdAndIsDeletedFalse(Long id);
}