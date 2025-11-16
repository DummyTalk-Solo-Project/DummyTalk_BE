package DummyTalk.DummyTalk_BE.domain.service.dummy.impl;

import DummyTalk.DummyTalk_BE.domain.converter.UserConverter;
import DummyTalk.DummyTalk_BE.domain.dto.ChatCompletionResponseDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyResponseDTO;
import DummyTalk.DummyTalk_BE.domain.dto.quiz.QuizResponseDTO;
import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.Quiz;
import DummyTalk.DummyTalk_BE.domain.entity.User;
import DummyTalk.DummyTalk_BE.domain.entity.constant.AIPrompt;
import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.UserQuiz;
import DummyTalk.DummyTalk_BE.domain.repository.DummyRepository;
import DummyTalk.DummyTalk_BE.domain.repository.QuizRepository;
import DummyTalk.DummyTalk_BE.domain.repository.UserQuizRepository;
import DummyTalk.DummyTalk_BE.domain.repository.UserRepository;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.DummyHandler;
import DummyTalk.DummyTalk_BE.global.exception.handler.UserHandler;
import DummyTalk.DummyTalk_BE.global.lock.DistributedLock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor
public class DummyServiceImplV3 {

    // 3. 동시성 관련 로직 or @Async 추가
    // Security 잠시 빼기

    private final UserRepository userRepository;
    private final DummyRepository dummyRepository;
    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final QuizRepository quizRepository;
    private final UserQuizRepository userQuizRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    @Value("${spring.ai.openai.api-key}")
    private String openAiKey;

    ///  TODO 개느려, 성능 개선할 것

    @Transactional
    public String GetDummyDateForNormal(String email, DummyRequestDTO.RequestInfoDTO requestInfoDTO) {

        String userContent, userInfo, newRequest = null;
        Random random = new Random();
        boolean isUserContent = false;

        User user = userRepository.findByEmailFetchInfoWithLock(email).orElseThrow(RuntimeException::new);

        if (user.getInfo().getReqCount() >= 10) {
            log.info("{} -> 무료 이용 횟수 모두 소모!", user.getEmail());
            throw new DummyHandler(ErrorCode.USED_ALL_CHANCES);
        }

        random.setSeed(System.currentTimeMillis());

        if (random.nextInt(3) == 0) { // 20%의 확률로
            try {
                userContent = objectMapper.writeValueAsString(requestInfoDTO);
                userInfo = objectMapper.writeValueAsString(UserConverter.toAIRequestDTO(user, user.getInfo()));
            } catch (JsonProcessingException e) {
                throw new DummyHandler(ErrorCode.PARSING_ERROR);
            }

            log.info("사용자의 정보를 사용합니다.");
            isUserContent = true;
            newRequest = AIPrompt.GET_DUMMY_PROMPT.concat("\n3. 다음은 사용자의 정보이다, 사용자 데이터 기반 잡상식을 만들 것, 위 사항은 정확히 따를 것" + email + ", " + userContent + ", userInfo: " + userInfo);
        }

        ChatResponse resp = chatModel.call(new Prompt(newRequest == null ? AIPrompt.GET_DUMMY_PROMPT : newRequest,
                OpenAiChatOptions.builder()
                        .model(OpenAiApi.ChatModel.GPT_4_O_MINI)
                        .maxTokens(100)
                        .build()));

        Dummy newDummy = Dummy.builder()
                .user(user)
                .isUserContent(isUserContent)
                .request(AIPrompt.GET_DUMMY_PROMPT)
                .response(resp.getResult().getOutput().getText())
                .build();
        dummyRepository.save(newDummy);

        user.getDummyList().add(newDummy);
        user.getInfo().updateReqCount();

        String text = resp.getResult().getOutput().getText();
        log.info("text result: {}", text);
        return text;
    }


    /**
     * 퀴즈를 만든 후 Redis 저장 및 캐시화
     *
     * @param email
     * @param openQuizDate
     */
    public void openQuiz(String email, LocalDateTime openQuizDate) {
        User user = userRepository.findByEmail(email).orElseThrow(RuntimeException::new);

        if (!Objects.equals(user.getEmail(), "jijysun@naver.com")) {
            throw new DummyHandler(ErrorCode.AUTHORIZATION_REQUIRED);
        }

        DummyRequestDTO.GetDummyQuizDTO dto = DummyRequestDTO.GetDummyQuizDTO.builder()
                .model("gpt-4o-mini")
                .messages(List.of(new DummyRequestDTO.Message("user",
                        AIPrompt.GET_QUIZ_PROMPT)))
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

        DummyResponseDTO.GetQuizFromAIResponseDTO responseDTO;
        try {
            responseDTO = objectMapper.readValue(text, DummyResponseDTO.GetQuizFromAIResponseDTO.class);
        } catch (JsonProcessingException e) {
            throw new DummyHandler(ErrorCode.AI_PARSING_ERROR);
        }

        log.info("responseDTO.toString(): {}", responseDTO);

        Quiz savedQuiz = quizRepository.save(Quiz.builder()
                .startTime(openQuizDate)
                .status(QuizStatus.OPEN)
                .title(responseDTO.getTitle())
                        .ticket(5)
                .answerList(responseDTO.getAnswerList())
                .description(responseDTO.getDescription())
                .answer(responseDTO.getAnswer())
                .startTime(openQuizDate)
                .endTime(openQuizDate.plusMinutes(3))
                .build());

        Map<String, Object> quizData = new HashMap<>();

        log.info("quiz: {}, {}", savedQuiz.getTitle(), savedQuiz.getDescription());

        quizData.put("id", savedQuiz.getId());
        quizData.put("status", savedQuiz.getStatus()); // 이거 필요한 가...? 어치피 만료될 거고
        quizData.put("title", savedQuiz.getTitle());
        quizData.put("description", savedQuiz.getDescription());
        quizData.put("ticket", savedQuiz.getTicket());
        quizData.put("answer", savedQuiz.getAnswer());
        quizData.put("answerList", savedQuiz.getAnswerList()); // json 타입
        quizData.put("startTime", savedQuiz.getStartTime().toString());
        quizData.put("endTime", savedQuiz.getStartTime().plusMinutes(3).toString());// 최대 3분동안

        redisTemplate.opsForHash().putAll("quiz", quizData);
        redisTemplate.expire("quiz", 24, TimeUnit.HOURS); // 안전장치, 해당 해쉬 키에 대한 만료 시간 정하기!!!
    }


    public DummyResponseDTO.GetQuizInfoResponseDTO getQuiz(String email) {

        Map<Object, Object> quiz = redisTemplate.opsForHash().entries("quiz");

        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserHandler(ErrorCode.CANT_FIND_USER));

        if (quiz.isEmpty()) {
            log.info("quiz is empty!"); // 사용자 별 이전 퀴즈 등수 확인
            Optional<UserQuiz> userQuiz = userQuizRepository.findLastestQuizByUserId(user.getId(), 1);

            if (userQuiz.isEmpty()) throw new DummyHandler(ErrorCode.NO_SOLVED_QUIZ);

            return DummyResponseDTO.GetQuizInfoResponseDTO.builder()
                    .status(QuizStatus.CLOSE)
                    .userGrade(userQuiz.get().getUserGrade())
                    .build();
        }

        QuizResponseDTO.QuizRedisDTO dto = objectMapper.convertValue(quiz, QuizResponseDTO.QuizRedisDTO.class);
        dto.setAnswerList((List<String>) quiz.get("answerList"));

        if (LocalDateTime.now().isBefore(dto.getStartTime())) {
            log.info("LocalDateTime.now(): {}", LocalDateTime.now());
            log.info("dto.getStartTime(): {}", dto.getStartTime());
            throw new DummyHandler(ErrorCode.QUIZ_NOT_OPEN);
        }

        // QuizStatus.OPEN
        log.info("return quiz!, {}, {}", dto.getTitle(), dto.getAnswerList());
        return DummyResponseDTO.GetQuizInfoResponseDTO.builder()
                .quizId(dto.getId())
                .title(dto.getTitle())
                .answerList(dto.getAnswerList())
                .build();
    }


    // 이전 기본 로직 메소드
    @Timed("quiz.solve.requests")
    public void solveQuiz(String email, Long quizId, Integer answer) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserHandler(ErrorCode.CANT_FIND_USER));

        Map<Object, Object> quiz = redisTemplate.opsForHash().entries("quiz");
        log.info("정답: {}, 제출 답안: {}", quiz.get("answer"), answer);

        if (quiz == null) {
            log.error("Wrong quiz!");
            throw new DummyHandler(ErrorCode.WRONG_QUIZ);
        }
        if (answer >= 5 || answer <= 0) {
            throw new DummyHandler(ErrorCode.WRONG_ANSWER);
        }
        if (redisTemplate.opsForHash().get("quiz", user.getId().toString()) != null) {
            log.error("{} -> already submit", email);
            throw new DummyHandler(ErrorCode.ALREADY_SUBMIT);
        }

        redisTemplate.opsForList().rightPush("quiz:answer", user.getId() + ":" + answer); // 따로 삭제 및 동기화 필요!!
        redisTemplate.opsForHash().put("quiz", user.getId().toString(), answer);
    }

    /**
     * Synchronized를 통한 동시성 해결 메소드
     * */
    @Timed("quiz.solve.requests")
    public synchronized void solveQuizVer2(String email, Integer answer) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserHandler(ErrorCode.CANT_FIND_USER));

        log.info("-- {}의 문제 풀이 작업 시작 --", email);

        Map<Object, Object> quiz = redisTemplate.opsForHash().entries("quiz");
        log.info("정답: {}, 제출 답안: {}", quiz.get("answer"), answer);

        if (quiz == null) {
            log.warn("Wrong quiz!");
            throw new DummyHandler(ErrorCode.WRONG_QUIZ);
        }
        if (answer >= 5 || answer <= 0) {
            throw new DummyHandler(ErrorCode.WRONG_ANSWER);
        }
        if (redisTemplate.opsForHash().get("quiz", user.getId().toString()) != null) {
            log.warn("{} -> already submit", email);
            throw new DummyHandler(ErrorCode.ALREADY_SUBMIT);
        }

        redisTemplate.opsForList().rightPush("quiz:answer", user.getId() + ":" + answer); // 따로 삭제 및 동기화 필요!!
        redisTemplate.opsForHash().put("quiz", user.getId().toString(), answer);

        log.info("-- {}의 문제 풀이 작업 종료 --", email);
    }

    /**
    * Redisson 분산 락 적용
    * */
    @DistributedLock(key = "'quiz'")
    @Timed("quiz.solve.requests")
    public void solveQuizVer3(String email, Integer answer) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserHandler(ErrorCode.CANT_FIND_USER));

        log.info("-- {}의 문제 풀이 작업 시작 --", email);

        Map<Object, Object> quiz = redisTemplate.opsForHash().entries("quiz");
        log.info("정답: {}, 제출 답안: {}", quiz.get("answer"), answer);

        if (quiz == null) {
            log.warn("Wrong quiz!");
            throw new DummyHandler(ErrorCode.WRONG_QUIZ);
        }
        if (answer >= 5 || answer <= 0) {
            throw new DummyHandler(ErrorCode.WRONG_ANSWER);
        }
        if (redisTemplate.opsForHash().get("quiz", user.getId().toString()) != null) {
            log.warn("{} -> already submit", email);
            throw new DummyHandler(ErrorCode.ALREADY_SUBMIT);
        }

        redisTemplate.opsForList().rightPush("quiz:answer", user.getId() + ":" + answer); // 따로 삭제 및 동기화 필요!!
        redisTemplate.opsForHash().put("quiz", user.getId().toString(), answer);

        log.info("-- {}의 문제 풀이 작업 종료 --", email);
    }


    /**
     * MySQL 단 락 적용
     * Quiz 엔티티의 ticket 에 대한 동시성 해결 메소드
     * */
    @Timed("quiz.solve.requests")
    @Transactional
    public void solveQuizVer4(DummyRequestDTO.SolveQuizReqDTO dto) {

        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new UserHandler(ErrorCode.CANT_FIND_USER));
        Quiz quiz = quizRepository.findQuizByIdForDecrease(dto.getQuizId()).orElseThrow(() -> new UserHandler(ErrorCode.WRONG_QUIZ));

        log.info("-- {}의 문제 풀이 작업 시작 --", user.getEmail());
        log.info("정답: {}, 제출 답안: {}", quiz.getAnswer(), dto.getAnswer());

        if (quiz == null) {
            log.warn("Wrong quiz!");
            throw new DummyHandler(ErrorCode.WRONG_QUIZ);
        }
        if (dto.getAnswer() >= 5 || dto.getAnswer() <= 0) {
            throw new DummyHandler(ErrorCode.WRONG_ANSWER);
        }

        // 중복 제출 방지
        if (redisTemplate.opsForHash().get("quiz", user.getId().toString()) != null) {
            log.warn("{} -> already submit", dto.getEmail());
            throw new DummyHandler(ErrorCode.ALREADY_SUBMIT);
        }
        redisTemplate.opsForHash().put("quiz", user.getId().toString(), dto.getAnswer());

        if (!quiz.decreaseTicket()){ // ek
            log.warn("{} -> quiz has NO TICKET!",  dto.getEmail());
        }

        log.info("-- {}의 문제 풀이 작업 종료 --", user.getEmail());
    }

}