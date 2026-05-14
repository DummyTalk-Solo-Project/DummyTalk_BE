package DummyTalk.DummyTalk_BE.global.scheduler.task;

import DummyTalk.DummyTalk_BE.domain.entity.Member;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberTask {

    private final MemberRepository memberRepository;

    @Transactional
    public void terminateExpiredMembers() {
        LocalDateTime cutoff = LocalDateTime.now().minusWeeks(2);
        List<Member> expiredMembers = memberRepository.findAllExpiredMembers(cutoff);

        if (expiredMembers.isEmpty()) {
            log.info("[MemberTask - terminateExpiredMembers()] - 삭제 대상 없음");
            return;
        }

        List<String> emails = expiredMembers.stream()
                .map(Member::getEmail)
                .toList();

        // cascade=ALL → Info, MemberDummy, MemberQuiz 자동 cascade 삭제
        memberRepository.deleteAll(expiredMembers);

        log.info("[MemberTask - terminateExpiredMembers()] - 영구 삭제 완료 ({}명): {}", emails.size(), emails);
    }
}