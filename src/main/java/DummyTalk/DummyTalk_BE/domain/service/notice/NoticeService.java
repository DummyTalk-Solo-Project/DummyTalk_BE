package DummyTalk.DummyTalk_BE.domain.service.notice;

import DummyTalk.DummyTalk_BE.domain.dto.notice.NoticeReqDTO;
import DummyTalk.DummyTalk_BE.domain.dto.notice.NoticeRespDTO;
import DummyTalk.DummyTalk_BE.domain.entity.Member;
import DummyTalk.DummyTalk_BE.domain.entity.Notice;
import DummyTalk.DummyTalk_BE.domain.entity.constant.MemberRole;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.MemberRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.NoticeRepository;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.MemberHandler;
import DummyTalk.DummyTalk_BE.global.exception.handler.NoticeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final MemberRepository memberRepository;

    private static final int PAGE_SIZE = 20;

    // ===================== 사용자용 =====================

    // 공개된 공지사항 목록 (isPublished=true)
    public List<NoticeRespDTO.NoticeListItemDTO> getPublishedNotices(int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        return noticeRepository
                .findByIsPublishedTrueAndIsDeletedFalseOrderByIsPinnedDescCreatedAtDesc(pageable)
                .map(NoticeRespDTO.NoticeListItemDTO::from)
                .getContent();
    }

    // 공개된 공지사항 상세 — isPublished=false 이면 NOTICE4002
    public NoticeRespDTO.NoticeDetailDTO getPublishedNotice(Long id) {
        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NoticeHandler(ErrorCode.NOTICE_NOT_FOUND));
        if (!notice.getIsPublished()) {
            throw new NoticeHandler(ErrorCode.NOTICE_NOT_PUBLISHED);
        }
        return NoticeRespDTO.NoticeDetailDTO.from(notice);
    }

    // ===================== Admin용 =====================

    // 전체 목록 — 비공개(임시저장) 포함
    public List<NoticeRespDTO.NoticeListItemDTO> getAllNotices(Long memberId, int page) {
        checkAdmin(memberId);
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        return noticeRepository
                .findByIsDeletedFalseOrderByIsPinnedDescCreatedAtDesc(pageable)
                .map(NoticeRespDTO.NoticeListItemDTO::from)
                .getContent();
    }

    // 상세 조회 — 비공개도 조회 가능
    public NoticeRespDTO.NoticeDetailDTO getNotice(Long memberId, Long id) {
        checkAdmin(memberId);
        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NoticeHandler(ErrorCode.NOTICE_NOT_FOUND));
        return NoticeRespDTO.NoticeDetailDTO.from(notice);
    }

    // 작성
    @Transactional
    public NoticeRespDTO.NoticeDetailDTO createNotice(Long memberId, NoticeReqDTO.CreateNoticeDTO dto) {
        Member admin = checkAdmin(memberId);
        Notice notice = Notice.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .isPinned(dto.getIsPinned() != null ? dto.getIsPinned() : false)
                .author(admin)
                .build();
        Notice saved = noticeRepository.save(notice);
        log.info("[NoticeService - createNotice()] - 공지사항 생성 | id={}, title={}", saved.getId(), saved.getTitle());
        return NoticeRespDTO.NoticeDetailDTO.from(saved);
    }

    // 수정 (부분 수정 — null 필드는 변경 안함)
    @Transactional
    public NoticeRespDTO.NoticeDetailDTO updateNotice(Long memberId, Long id, NoticeReqDTO.UpdateNoticeDTO dto) {
        checkAdmin(memberId);
        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NoticeHandler(ErrorCode.NOTICE_NOT_FOUND));
        notice.update(dto.getTitle(), dto.getContent(), dto.getIsPinned());
        log.info("[NoticeService - updateNotice()] - 공지사항 수정 | id={}", id);
        return NoticeRespDTO.NoticeDetailDTO.from(notice);
    }

    // 삭제 (soft delete)
    @Transactional
    public Boolean deleteNotice(Long memberId, Long id) {
        checkAdmin(memberId);
        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NoticeHandler(ErrorCode.NOTICE_NOT_FOUND));
        notice.delete();
        log.info("[NoticeService - deleteNotice()] - 공지사항 삭제 | id={}", id);
        return true;
    }

    // 공개/비공개 토글 — 변경 후 현재 isPublished 반환
    @Transactional
    public Boolean toggleNoticePublish(Long memberId, Long id) {
        checkAdmin(memberId);
        Notice notice = noticeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NoticeHandler(ErrorCode.NOTICE_NOT_FOUND));
        notice.togglePublish();
        log.info("[NoticeService - toggleNoticePublish()] - 공지사항 공개 상태 변경 | id={}, isPublished={}", id, notice.getIsPublished());
        return notice.getIsPublished();
    }

    // Admin 권한 체크 공통 — Member 반환 (작성자 저장 등 후속 사용)
    private Member checkAdmin(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getRole().equals(MemberRole.MEMBER)) {
            throw new MemberHandler(ErrorCode.AUTH_FORBIDDEN);
        }
        return member;
    }
}