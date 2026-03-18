package DummyTalk.DummyTalk_BE.global.security.userDetails;

import DummyTalk.DummyTalk_BE.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException { // -> email!

        return memberRepository.findByEmail(username)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new RuntimeException("Cant find USER!!!!!!"));
    }
}
