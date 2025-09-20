package DummyTalk.DummyTalk_BE.domain.service.dummy;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.entity.User;

public interface DummyService {

    String GetDummyDateForNormal(User user, DummyRequestDTO.RequestInfoDTO requestInfoDTO);


}
