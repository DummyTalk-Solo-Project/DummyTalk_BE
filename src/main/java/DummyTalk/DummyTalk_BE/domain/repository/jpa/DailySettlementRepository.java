package DummyTalk.DummyTalk_BE.domain.repository.jpa;

import DummyTalk.DummyTalk_BE.domain.entity.DailySettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailySettlementRepository extends JpaRepository<DailySettlement, Long> {

    Optional<DailySettlement> findBySettlementDate(LocalDate settlementDate);

    // 기간별 정산 목록 — from~to 범위, 날짜 오름차순
    List<DailySettlement> findBySettlementDateBetweenOrderBySettlementDateAsc(LocalDate from, LocalDate to);
}