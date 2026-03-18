package DummyTalk.DummyTalk_BE.global.security.userDetails;

import DummyTalk.DummyTalk_BE.domain.entity.Member;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Member member; // @AuthenticationPrincipal

    /*
     * isAccountNonExpired(), isAccountNonLocked(), isCredentialsNonExpired(), isEnabled()
     * -> 서비스 상 관련 기획이 존재하지 않으므로 return true로 변경.
     * */

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(member.getRole().name()));
    }

    public Member getMember (){
        return member;
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        return member.getEmail(); // 고유 값인 이메일 반환!!
    }

    @Override
    public boolean isAccountNonExpired() {
//        return UserDetails.super.isAccountNonExpired();
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
//        return UserDetails.super.isAccountNonLocked();
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
//        return UserDetails.super.isCredentialsNonExpired();
        return true;
    }

    @Override
    public boolean isEnabled() {
//        return UserDetails.super.isEnabled();
        return true;
    }
}
