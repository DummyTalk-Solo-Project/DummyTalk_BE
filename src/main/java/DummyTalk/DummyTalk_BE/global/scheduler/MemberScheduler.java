package DummyTalk.DummyTalk_BE.global.scheduler;

import DummyTalk.DummyTalk_BE.domain.entity.Rarity;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.MemberDummyRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.MemberRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.RarityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class MemberScheduler {

    private final MemberDummyRepository memberDummyRepository;
    private final MemberRepository memberRepository;
    private final RarityRepository rarityRepository;
    private final RedisTemplate redisTemplate;


    ///TODO 탈퇴 후 2주 초과 회원 영구 삭제 스케줄러 미구현
    ///     삭제 순서: Info → MemberQuiz → MemberDummy → Member (FK 의존성 역순)
    ///     조건: isDeleted=true AND deletedAt < now()-2weeks
    ///     주기: @Scheduled(cron = "0 0 2 * * *") 권장

    /**
     * 관리자용 사이트 정산
     * */
    @Scheduled(cron = "0 30 0 * * *")
    public void calculateDummy (){
        log.info("Calculating Dummy Job");

        // 어제 000000 ~ 235957
        LocalDateTime createdAtAfter = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime createdAtBefore = LocalDate.now().minusDays(1).atStartOfDay().withMinute(59).withSecond(57);
        List<Long> memberDummyIdList = memberDummyRepository.findAllByCreatedAtToday(
                createdAtAfter,
                createdAtBefore);

        // 총 Dummy 조회 수
        long memberDummyCount = memberDummyIdList.size();

        // 신규 가입자.
        long newMemberCount = memberRepository.countMemberByCreatedAtBetween(createdAtAfter, createdAtBefore);

        // 등급 별 Dummy 조회 정산
        AtomicInteger commonCount = new AtomicInteger();
        AtomicInteger rareCount = new AtomicInteger();
        AtomicInteger epicCount = new AtomicInteger();
        AtomicInteger specialCount = new AtomicInteger();


        rarityRepository.findAllByIdList(memberDummyIdList).forEach(r -> {
            switch (r.getName()){
                case COMMON:
                    commonCount.getAndIncrement();
                    break;
                case RARE:
                    rareCount.getAndIncrement();
                    break;
                case EPIC:
                    epicCount.getAndIncrement();
                    break;
                case SPECIAL:
                    specialCount.getAndIncrement();
                    break;
                default:
                    break;
            }
        });

        ///TODO 정산 결과 처리 미구현 — DB 저장(통계 테이블), Redis 캐시, 관리자 대시보드 API 응답 등 방향 결정 후 구현 필요
        // return WHAT!?
    }
}
