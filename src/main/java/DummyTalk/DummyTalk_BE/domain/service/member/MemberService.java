package DummyTalk.DummyTalk_BE.domain.service.member;

import DummyTalk.DummyTalk_BE.domain.dto.member.MemberRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.member.MemberResponseDTO;

import java.util.List;

public interface MemberService {

    void sendVerificationEmail(String email);

    void verifyEmail(MemberRequestDTO.VerificationRequestDTO requestDTO);

    void signIn(MemberRequestDTO.SignInRequestDTO request);

    MemberResponseDTO.LoginSuccessDTO login(MemberRequestDTO.LoginRequestDTO requestDTO);

    void withdraw (String email);

    List<MemberResponseDTO.GetUserResponseDTO> getAllData();
}
