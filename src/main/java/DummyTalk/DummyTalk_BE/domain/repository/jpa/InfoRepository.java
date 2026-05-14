package DummyTalk.DummyTalk_BE.domain.repository.jpa;

import DummyTalk.DummyTalk_BE.domain.entity.Info;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InfoRepository extends JpaRepository<Info, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Info i WHERE i.member.email = :email")
    void deleteByEmail(@Param("email") String email);

    // isSubscribe=true AND subsExprDate < now → 만료 대상
    @Query("SELECT i FROM Info i WHERE i.isSubscribe = true AND i.subsExprDate IS NOT NULL AND i.subsExprDate < :now AND i.member.isDeleted = false")
    List<Info> findAllExpiredSubscriptions(@Param("now") LocalDateTime now);


}
