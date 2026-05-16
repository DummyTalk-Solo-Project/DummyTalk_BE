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

}
