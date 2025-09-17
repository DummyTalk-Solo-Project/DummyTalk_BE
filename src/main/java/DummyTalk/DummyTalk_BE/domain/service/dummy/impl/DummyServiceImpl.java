package DummyTalk.DummyTalk_BE.domain.service.dummy.impl;

import DummyTalk.DummyTalk_BE.domain.converter.UserConverter;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.entity.dummy.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.user.User;
import DummyTalk.DummyTalk_BE.domain.repository.DummyRepository;
import DummyTalk.DummyTalk_BE.domain.repository.UserRepository;
import DummyTalk.DummyTalk_BE.domain.service.dummy.DummyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;


@Slf4j
@Service
@RequiredArgsConstructor
public class DummyServiceImpl implements DummyService {

    private final UserRepository userRepository;
    private final DummyRepository dummyRepository;
    private final OpenAiChatModel chatModel;
    private final ObjectMapper mapper;

    String content = "현 요청은 메타픽션 Spring 사이드 프로젝트에서 비회원 사용자가 잡상식을 구하는 요청이다.\n" +
            "해당 웹 사이트의 컨셉은 계속해서 새로고침 하다가 보면 일정 확률로 사용자 기반 데이터를 가지고 잡상식을 요청하면서 메타픽션을 다루게 될 것.\n" +
            "사전 설정을 일단 잘 알아두고, 수많은 주제에 대한 랜덤의 잡상식을 요청한다. 응답 잡상식은 다음 사항을 무조건 따라야 한다.\n" +
            "1. 답변은 최소 90자 이상 120 글자 내로 답해야 한다, 또한 문장를 완벽하게 끝마무리 지어야 한다.\n" +
            "2. 답변의 말투는 ~~요를 사용하여 친근하면서도 차갑지 않은 중립적의 말투를 사용할 것\n";

    @Override
    public String getDummyDataForGuest(DummyRequestDTO.RequestInfoDTO requestInfoDTO) {
        return "";
    }

    @Override
    @Transactional
    public String GetDummyDateForNormal(User reqUser, DummyRequestDTO.RequestInfoDTO requestInfoDTO) {

        String userContent, userInfo, newRequest = null;
        Random random = new Random();
        boolean isUserContent = false;

        log.info("{}", reqUser.toString());

        User user = userRepository.findByEmail(reqUser.getEmail()).orElseThrow(RuntimeException::new);

        if (user.getInfo().getReqCount() >= 10){
            log.info("{} -> 무료 이용 횟수 모두 소모!", user.getEmail());
            return "무료 이용 횟수를 모두 이용하셨네요. 다음을 기약해주세요 :)";
        }


        random.setSeed(System.currentTimeMillis());

        if (random.nextInt(3) == 0){ // 20%의 확률로
            try {
                userContent = mapper.writeValueAsString(requestInfoDTO);
                userInfo = mapper.writeValueAsString(UserConverter.toAIRequestDTO(user, user.getInfo()));

                log.info("userContent: {}", userContent);
                log.info("userInfo: {}", userInfo);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            log.info("사용자의 정보를 사용합니다.");
            isUserContent = true;
            newRequest = content.concat("\n3. 다음은 사용자의 정보이다, 사용자 데이터 기반 잡상식을 만들 것, 위 사항은 정확히 따를 것" + reqUser + ", " + userContent + ", userInfo: " + userInfo);
        }

        ChatResponse resp = chatModel.call(new Prompt(newRequest == null ? content:newRequest,
                OpenAiChatOptions.builder()
                        .model(OpenAiApi.ChatModel.GPT_4_O)
                        .maxTokens(100)
                        .build()));

        Dummy newDummy = Dummy.builder()
                .user(user)
                .isUserContent(isUserContent)
                .request(content)
                .response(resp.getResult().getOutput().getText())
                .build();
        dummyRepository.save(newDummy);

        user.getDummyList().add(newDummy);
        user.getInfo().updateReqCount();

        String text = resp.getResult().getOutput().getText();
        log.info("text result: {}", text);
        return text;
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
