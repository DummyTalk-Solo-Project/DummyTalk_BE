package DummyTalk.DummyTalk_BE.domain.service.chat.impl;

import DummyTalk.DummyTalk_BE.domain.service.chat.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Override
    public String GetDummyData() {


        return "";
    }

}
