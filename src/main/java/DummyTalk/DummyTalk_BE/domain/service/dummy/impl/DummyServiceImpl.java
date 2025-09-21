package DummyTalk.DummyTalk_BE.domain.service.dummy.impl;

import DummyTalk.DummyTalk_BE.domain.converter.UserConverter;
import DummyTalk.DummyTalk_BE.domain.dto.ChatCompletionResponseDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyResponseDTO;
import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.Quiz;
import DummyTalk.DummyTalk_BE.domain.entity.User;
import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.UserQuiz;
import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.User_Quiz;
import DummyTalk.DummyTalk_BE.domain.repository.DummyRepository;
import DummyTalk.DummyTalk_BE.domain.repository.QuizRepository;
import DummyTalk.DummyTalk_BE.domain.repository.QuizRepository;
import DummyTalk.DummyTalk_BE.domain.repository.UserQuizRepository;
import DummyTalk.DummyTalk_BE.domain.repository.UserRepository;
import DummyTalk.DummyTalk_BE.domain.service.dummy.DummyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;


@Slf4j
@Service
@RequiredArgsConstructor
public class DummyServiceImpl implements DummyService {

    private final QuizRepository quizRepository;
    @Value("${spring.ai.openai.api-key}")
    private String openAiKey;

    private final UserRepository userRepository;
    private final DummyRepository dummyRepository;
    private final QuizRepository quizRepository;
    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final ObjectMapper mapper;
    private final UserQuizRepository userQuizRepository;
    private final RedisTemplate<Object, Object> redisTemplate;

    String content = "현 요청은 메타픽션 Spring 프로젝트에서 비회원 사용자가 잡상식을 구하는 요청이다. \n 사이트 컨셉은 계속해서 새로고침 하다가 보면 일정 확률로 사용자 기반 데이터를 가지고 잡상식을 요청하면서 메타픽션을 다루게 될 것.\n" +
            "사전 설정을 일단 잘 알아두고, 수많은 주제에 대한 랜덤의 잡상식을 요청한다. 응답 잡상식은 다음 사항을 무조건 따라야 한다.\n" +
            "1. 답변은 최소 90자 이상 120 글자 내로 답해야 한다, 또한 문장를 완벽하게 끝마무리 지어야 한다.\n" +
            "2. 답변의 말투는 ~~요를 사용하여 친근하면서도 차갑지 않은 중립적의 말투를 사용할 것\n" +
            "3. 또한 사용자 데이터 사용 시 사용자를 아는 척 하지 말고, ";

    ///  TODO 개느려, 성능 개선할 것
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
                userContent = objectMapper.writeValueAsString(requestInfoDTO);
                userInfo = objectMapper.writeValueAsString(UserConverter.toAIRequestDTO(user, user.getInfo()));

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
    public void openQuiz(User reqUser, LocalDateTime openQuizDate) {
        User user = userRepository.findByEmail(reqUser.getEmail()).orElseThrow(RuntimeException::new);

        if (!Objects.equals(user.getEmail(), "jijysun@naver.com")){
            throw new RuntimeException("권한이 부족합니다.");
        }

        DummyRequestDTO.GetDummyQuizDTO dto = DummyRequestDTO.GetDummyQuizDTO.builder()
                .model("gpt-4o-mini")
                .messages(List.of(new DummyRequestDTO.Message("user",
                        "헷갈릴 수 있는 약간 어려운 잡학상식 문제를 낼 것, 다음 요청사항을 정확히 따를 것  ->  " +
                                "1. 답변은 JSON 형식에 맞춰 답변할 것 {\"title\": \"니가 내는 문제\", \"answerList\": [\"정답1\", \"정답2\", \"정답3\", \"정답4\"], \"answer\": 정답번호, \"description\": \"정답에 대한 설명\"}, " +
                                "2. 답변 말투는 ~요?를 사용하여 친근하면서도 차갑지 않은 중립적의 말투를 사용할 것, " +
                                "3. 답변 속의 문제와 4개의 보기는 title과 answerList, answer에 정확히 담을 것, 또한 ```json 등의 코드 블록 문자도 제거할 것, " +
                                "4. 최소 70 글자 이상의 문제를 낼 것. 이 규칙을 제일 중요하게 여길 것. " +
                                "5. 정답에 대한 설명을 간단하게 적고 description 에 담을 것, " +
                        "이후 JSON은 Java class {String title, List<String> answerList, Integer answer} 로 파싱할 예정이니 형식을 엄수할 것")))
                .max_tokens(200)
                .build();

        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String text = webClient.post()
                .uri("/chat/completions")
                .bodyValue(dto)
                .retrieve()
                .bodyToFlux(ChatCompletionResponseDTO.class)
                .map(resp -> resp.getChoices().get(0).getMessage().getContent())
                        .blockLast();

        DummyResponseDTO.GetDummyQuizResponseDTO responseDTO;
        try {
            responseDTO = objectMapper.readValue(text, DummyResponseDTO.GetDummyQuizResponseDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI 퀴즈 파싱 도중 오류가 발생하였습니다!");
        }

        log.info("responseDTO.toString(): {}", responseDTO);

        quizRepository.save(Quiz.builder()
                .startTime(openQuizDate)
                .status(QuizStatus.OPEN)
                .title(responseDTO.getTitle())
                .answerList(responseDTO.getAnswerList())
                .description(responseDTO.getDescription())
                .answer(responseDTO.getAnswer())
                .build());

    }
}
    @Override
    public DummyResponseDTO.GetQuizResponseDTO getQuiz(User user) {

        Quiz quiz = quizRepository.findLastestQuiz();
        if (quiz.getStatus().equals(QuizStatus.NOT_OPEN) || LocalDateTime.now().isBefore(quiz.getStartTime())) {
            // 아직 퀴즈가 열리지 않았습니다.
            throw new RuntimeException("퀴즈가 아직 열리지 않았습니다.");
        } else if (quiz.getStatus().equals(QuizStatus.CLOSE)) {
            // 사용자 별 이전 퀴즈 등수 확인
            Optional<User_Quiz> userQuiz = userQuizRepository.findLastestQuizByUserId(user.getId(), 1);

            if (userQuiz.isEmpty()) throw new RuntimeException("해당 사용자가 풀었던 문제가 없습니다.");

//            return userQuiz.get().getUserGrade();
        }

        return DummyResponseDTO.GetQuizResponseDTO.builder()
//                    .title(quiz.getTitle())
//                    .answerList(quiz.getAnswerList())
                .build();
    }

    @Override
    public void solveQuiz(User user, Integer answer) {

        Quiz quiz = quizRepository.findLastestQuiz();

        /// TODO 만든 퀴즈에 대해서는 빠른 접근을 통해 Redis화 할 것.

        redisTemplate.opsForHash().put("quiz:"+quiz.getId(), "user:"+user.getId()+"answer", answer); // 퀴즈의 사용자 별 답안
        redisTemplate.opsForHash().increment("quiz:"+quiz.getId(), "solutionCount", 1);

        // 제한도 있지만 일단 열어두기
/*        if ((Integer)redisTemplate.opsForHash().get("quiz:"+quiz.getId(), "solutionCount") >= 3){
            return;
        }*/
    }

}