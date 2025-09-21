package DummyTalk.DummyTalk_BE.domain.service.dummy;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyResponseDTO;
import DummyTalk.DummyTalk_BE.domain.entity.User;

public interface DummyService {

    String getDummyDataForGuest (DummyRequestDTO.RequestInfoDTO requestInfoDTO);

    String GetDummyDateForNormal(User user, DummyRequestDTO.RequestInfoDTO requestInfoDTO);

    String GetDummyDateForAdvanced (User user, DummyRequestDTO.RequestInfoDTO requestInfoDTO);

    String GetDummyDateForDanger(User user, DummyRequestDTO.RequestInfoDTO requestInfoDTO);

    DummyResponseDTO.GetQuizResponseDTO getQuiz(User user);

}
