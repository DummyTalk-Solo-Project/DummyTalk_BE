package DummyTalk.DummyTalk_BE.global.scheduler;

import DummyTalk.DummyTalk_BE.global.scheduler.task.AdminTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminScheduler {

    private final AdminTask adminTask;

    // 매일 자정 30분: 어제 데이터 정산 후 daily_settlement 테이블에 저장
    // 사용자 관련 스케줄러(MemberScheduler)와 분리하여 관리자 정산 단독 운영
    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Seoul")
    public void calculateDummy() {
        log.info("[AdminScheduler - calculateDummy()] - 일일 정산 시작");
        adminTask.calculateAndSaveDailySettlement();
    }
}