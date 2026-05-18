package DummyTalk.DummyTalk_BE.domain.service.admin;

import DummyTalk.DummyTalk_BE.domain.dto.admin.AdminRespDTO;
import DummyTalk.DummyTalk_BE.domain.entity.DailySettlement;
import DummyTalk.DummyTalk_BE.domain.entity.Member;
import DummyTalk.DummyTalk_BE.domain.entity.constant.MemberRole;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.DailySettlementRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.MemberRepository;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.AdminHandler;
import DummyTalk.DummyTalk_BE.global.exception.handler.MemberHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final DailySettlementRepository dailySettlementRepository;
    private final MemberRepository memberRepository;

    // 특정 날짜의 정산 데이터 단건 조회
    // AdminTask 가 매일 00:30 에 전날치를 저장하므로 오늘 날짜는 조회 불가
    @Transactional(readOnly = true)
    public AdminRespDTO.DailySettlementRespDTO getDailySettlement(Long memberId, LocalDate date) {

        // 기존 서비스 계층과 동일한 Admin 권한 체크 패턴
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getRole().equals(MemberRole.MEMBER)) {
            throw new MemberHandler(ErrorCode.AUTH_FORBIDDEN);
        }

        DailySettlement settlement = dailySettlementRepository.findBySettlementDate(date)
                .orElseThrow(() -> new AdminHandler(ErrorCode.SETTLEMENT_NOT_FOUND));

        log.info("[AdminService - getDailySettlement()] - 정산 조회 | 날짜={}", date);
        return AdminRespDTO.DailySettlementRespDTO.from(settlement);
    }
}