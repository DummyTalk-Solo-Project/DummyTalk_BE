package DummyTalk.DummyTalk_BE.domain.converter;

import DummyTalk.DummyTalk_BE.domain.dto.member.MemberReqDTO;
import DummyTalk.DummyTalk_BE.domain.entity.constant.Login;
import DummyTalk.DummyTalk_BE.domain.entity.Info;
import DummyTalk.DummyTalk_BE.domain.entity.Member;

public class UserConverter {

    public static Member toNewUser (MemberReqDTO.SignInRequestDTO request) {
        return Member.builder()
                .memberName(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .login(Login.NORMAL)
                .build();
    }

    public static MemberReqDTO.AIRequestDTO toAIRequestDTO (Member member, Info info){
        return MemberReqDTO.AIRequestDTO.builder()
                .email(member.getEmail())
                .username(member.getMemberName())
                .login(member.getLogin())
                .info(info)
                .build();

    }
}
