package DummyTalk.DummyTalk_BE.domain.service.dummy.impl;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.entity.user.User;
import DummyTalk.DummyTalk_BE.domain.repository.UserRepository;
import DummyTalk.DummyTalk_BE.domain.service.dummy.DummyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class DummyServiceImpl implements DummyService {

    private final UserRepository userRepository;
    private final OpenAiChatModel chatModel;

    @Override
    public String getDummyDataForGuest(DummyRequestDTO.RequestInfoDTO requestInfoDTO) {
        ChatResponse resp = chatModel.call(new Prompt("spring ai 를 처음 써서 너한테 테스트로 보내는 중이야. 그리고 200 토큰 제한이 설정되었는지 얘기해주고, 해당 답변은 얼마나 토큰을 사용했는 지 얘기할 것",
                OpenAiChatOptions.builder()
                        .model(OpenAiApi.ChatModel.GPT_4_O)
                        .maxCompletionTokens(200)
                        .build()));

        String text = resp.getResult().getOutput().getText();
        log.info("text result: {}", text);
        return "";
    }

    @Override
    public String GetDummyDateForNormal(User user, DummyRequestDTO.RequestInfoDTO requestInfoDTO) {

        ChatResponse resp = chatModel.call(new Prompt("spring ai 를 처음 써서 너한테 테스트로 보내는 중이야",
                OpenAiChatOptions.builder()
                        .model(OpenAiApi.ChatModel.GPT_4_O)
                        .maxTokens(200)
                        .build()));

        String text = resp.getResult().getOutput().getText();
        log.info("text result: {}", text);
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
