package DummyTalk.DummyTalk_BE.global.scheduler;

import DummyTalk.DummyTalk_BE.global.scheduler.task.MemberTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberScheduler {

    private final MemberTask memberTask;

    // reqCount 초기화: 구독/미구독 모든 회원 매일 자정 0으로 리셋
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void resetReqCounts() {
        log.info("[MemberScheduler - resetReqCounts()] - reqCount 초기화 시작");
        memberTask.resetAllReqCounts();
    }

    // 구독 만료 처리: subsExprDate < now 인 회원 isSubscribe → false (reqCount 초기화 직후 실행)
    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Seoul")
    public void expireSubscriptions() {
        log.info("[MemberScheduler - expireSubscriptions()] - 구독 만료 처리 시작");
        memberTask.expireSubscriptions();
    }

    @Scheduled(cron = "0 3 0 * * *", zone = "Asia/Seoul")
    public void terminateMember() {
        log.info("[MemberScheduler - terminateMember()] - 탈퇴 2주 초과 회원 영구 삭제 시작");
        memberTask.terminateExpiredMembers(); // Spring Proxy AOP로 인한 트랜잭션 문제로 Task 분리.
    }
}