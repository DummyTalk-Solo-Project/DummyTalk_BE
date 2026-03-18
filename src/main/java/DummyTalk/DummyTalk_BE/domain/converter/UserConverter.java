package DummyTalk.DummyTalk_BE.domain.converter;

import DummyTalk.DummyTalk_BE.domain.dto.member.MemberRequestDTO;
import DummyTalk.DummyTalk_BE.domain.entity.constant.Login;
import DummyTalk.DummyTalk_BE.domain.entity.Info;
import DummyTalk.DummyTalk_BE.domain.entity.Member;

public class UserConverter {

    public static Member toNewUser (MemberRequestDTO.SignInRequestDTO request) {
        return Member.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .login(Login.NORMAL)
                .build();
    }

    public static MemberRequestDTO.AIRequestDTO toAIRequestDTO (Member member, Info info){
        return MemberRequestDTO.AIRequestDTO.builder()
                .email(member.getEmail())
                .username(member.getUsername())
                .login(member.getLogin())
                .info(info)
                .build();

    }
}
