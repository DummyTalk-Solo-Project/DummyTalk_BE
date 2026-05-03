package DummyTalk.DummyTalk_BE.global.security.userDetails;

import DummyTalk.DummyTalk_BE.domain.repository.jpa.MemberRepository;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService { // 상속으로 자체적인 DB 조회

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // username 이지만, 내부에는 Email - 사용자 별 고유 값이 들어있음!

        return memberRepository.findByEmail(username)
                .map(CustomUserDetails::new) // 이후 @AuthenticationPrincipal 사용으로 Controller 계층에서 받도록
                .orElseThrow(() -> new GeneralException(ErrorCode.MEMBER_NOT_FOUND) ); // 일단 GeneralException으로?
    }
}
