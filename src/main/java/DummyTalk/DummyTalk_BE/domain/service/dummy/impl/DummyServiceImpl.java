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

    String content = "현 요청은 메타픽션 Spring 사이드 프로젝트에서 비회원 사용자가 잡상식을 구하는 요청이다.\n" +
            "해당 웹 사이트의 컨셉은 계속해서 새로고침 하다가 보면 일정 확률로 사용자 기반 데이터를 가지고 잡상식을 요청하면서 메타픽션을 다루게 될 것.\n" +
            "1. 답변은 80자 이상 100 글자 내로, {content: 너가 답변할 내용, 그리고 마지막에 글자 수를 붙힐 것} JSON 형식으로 답할 것\n" +
            "2. 답변의 말투는 ~~요를 사용하여 친근하면서도 차갑지 않은 중립적의 말투를 사용할 것\n" +
            "3. ObjectMapper 로 파싱할 예정이니, ```json 등의 불필요한 문자는 절대로 넣지 말것";

    @Override
    public String getDummyDataForGuest(DummyRequestDTO.RequestInfoDTO requestInfoDTO) {
        ChatResponse resp = chatModel.call(new Prompt(content,
                OpenAiChatOptions.builder()
                        .model(OpenAiApi.ChatModel.GPT_4_O)
                        .maxTokens(100)
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
