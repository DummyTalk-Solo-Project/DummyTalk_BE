package DummyTalk.DummyTalk_BE.domain.service.dummy.impl;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.entity.user.User;
import DummyTalk.DummyTalk_BE.domain.repository.UserRepository;
import DummyTalk.DummyTalk_BE.domain.service.dummy.DummyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class DummyServiceImpl implements DummyService {

    private final UserRepository userRepository;
    private final OpenAiApi openAi;

    @Override
    public String GetDummyDateForNormal(User user, DummyRequestDTO.RequestInfoDTO requestInfoDTO) {

        return "";
    }

    @Override
    public String GetDummyDateForAdvanced(User user, DummyRequestDTO.RequestInfoDTO requestInfoDTO) {
        return "";
    }

    @Override
    public String GetDummyDateForDanger(User user, DummyRequestDTO.RequestInfoDTO requestInfoDTO) {
        return "";
    }

}
