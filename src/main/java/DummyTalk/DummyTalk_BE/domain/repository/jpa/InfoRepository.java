package DummyTalk.DummyTalk_BE.domain.repository.jpa;

import DummyTalk.DummyTalk_BE.domain.entity.Info;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InfoRepository extends JpaRepository<Info, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Info i WHERE i.member.email = :email")
    void deleteByEmail(@Param("email") String email);

    // isSubscribe=true AND subsExprDate < now → 만료 대상
    @Query("SELECT i FROM Info i WHERE i.isSubscribe = true AND i.subsExprDate IS NOT NULL AND i.subsExprDate < :now AND i.member.isDeleted = false")
    List<Info> findAllExpiredSubscriptions(@Param("now") LocalDateTime now);

    // reqCount 초기화 대상: 탈퇴하지 않은 모든 활성 회원
    @Query("SELECT i FROM Info i WHERE i.member.isDeleted = false")
    List<Info> findAllActiveInfos();

    // 정산 시점 현재 구독자 수
    @Query("SELECT COUNT(i) FROM Info i WHERE i.isSubscribe = true AND i.member.isDeleted = false")
    long countActiveSubscribers();

    // 퀴즈 보상 지급 시 memberId로 구독 정보 조회
    Optional<Info> findByMember_Id(Long memberId);

    // ─── K6 부하 테스트 지원 (AdminService.resetLoadTestUsers / getLoadTestState) ───
    // 대상은 TestMemberDataLoader가 만든 test%@test.com 유저로 한정 — 실제 회원 데이터는 건드리지 않음
    //
    // reqCount=0 : 회차 사이 일일 한도 초기화 (한도 도달 시 DUMMY_4001로 DB 갱신 없이 반환되어 측정 경로 이탈)
    // isSubscribe=true : 구독자 한도 40회 적용 — 5s 주기 × 3분 = 유저당 36회라 비구독(20)로는 회차 완주 불가
    //                    구독 분기는 DummyService에서 한도 상수만 다르고 락/갱신 경로는 동일 (측정 왜곡 없음)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Info i SET i.reqCount = 0, i.isSubscribe = true WHERE i.member.email LIKE 'test%@test.com'")
    int resetLoadTestUsers();

    // Lost Update ground truth: 회차 전후 Δ(SUM reqCount) 가 k6의 200 성공 수와 같아야 함 (작으면 갱신 유실)
    @Query("SELECT COALESCE(SUM(i.reqCount), 0) FROM Info i WHERE i.member.email LIKE 'test%@test.com'")
    long sumReqCountOfLoadTestUsers();

    @Query("SELECT COUNT(i) FROM Info i WHERE i.member.email LIKE 'test%@test.com'")
    long countLoadTestUsers();

}
