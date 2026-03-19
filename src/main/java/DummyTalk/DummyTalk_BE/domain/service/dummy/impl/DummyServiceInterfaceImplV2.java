package DummyTalk.DummyTalk_BE.domain.service.dummy.impl;

import DummyTalk.DummyTalk_BE.domain.converter.UserConverter;
import DummyTalk.DummyTalk_BE.domain.dto.ChatCompletionResponseDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyResponseDTO;
import DummyTalk.DummyTalk_BE.domain.dto.quiz.QuizResponseDTO;
import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.Quiz;
import DummyTalk.DummyTalk_BE.domain.entity.Member;
import DummyTalk.DummyTalk_BE.domain.entity.constant.AIPrompt;
import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberQuiz;
import DummyTalk.DummyTalk_BE.domain.repository.DummyRepository;
import DummyTalk.DummyTalk_BE.domain.repository.QuizRepository;
import DummyTalk.DummyTalk_BE.domain.repository.MemberQuizRepository;
import DummyTalk.DummyTalk_BE.domain.repository.MemberRepository;
import DummyTalk.DummyTalk_BE.domain.service.dummy.DummyServiceInterface;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.DummyHandler;
import DummyTalk.DummyTalk_BE.global.exception.handler.UserHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Slf4j
//@Service
@RequiredArgsConstructor
public class DummyServiceInterfaceImplV2 implements DummyServiceInterface {

    // 2. 퀴즈 조회 로직 -> Redis 캐싱을 통한 빠른 조회 도입

    private final MemberRepository memberRepository;
    private final DummyRepository dummyRepository;
    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final QuizRepository quizRepository;
    private final MemberQuizRepository memberQuizRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.ai.openai.api-key}")
    private String openAiKey;

    ///  TODO 개느려, 성능 개선할 것
    @Override
    @Transactional
    public String GetDummyDateForNormal(Member reqMember, DummyRequestDTO.RequestInfoDTO requestInfoDTO) {

/*
        */
/* 처리 중 로직 구현하기. (Redis SETNX )*//*


        String userContent, userInfo, newRequest = null;
        Random random = new Random();
        boolean isUserContent = false;

        log.info("{}", reqMember.toString());

        Member member = memberRepository.findByEmailFetchInfoWithLock(reqMember.getEmail()).orElseThrow(RuntimeException::new);

        if (member.getInfo().getReqCount() >= 10) {
            log.info("{} -> 무료 이용 횟수 모두 소모!", member.getEmail());
            throw new DummyHandler(ErrorCode.USED_ALL_CHANCES);
        }

        random.setSeed(System.currentTimeMillis());

        if (random.nextInt(3) == 0) { // 20%의 확률로
            try {
                userContent = objectMapper.writeValueAsString(requestInfoDTO);
                userInfo = objectMapper.writeValueAsString(UserConverter.toAIRequestDTO(member, member.getInfo()));
            } catch (JsonProcessingException e) {
                throw new DummyHandler(ErrorCode.PARSING_ERROR);
            }

            log.info("사용자의 정보를 사용합니다.");
            isUserContent = true;
            newRequest = AIPrompt.GET_DUMMY_PROMPT.concat("\n3. 다음은 사용자의 정보이다, 사용자 데이터 기반 잡상식을 만들 것, 위 사항은 정확히 따를 것" + reqMember + ", " + userContent + ", userInfo: " + userInfo);
        }

        ChatResponse resp = chatModel.call(new Prompt(newRequest == null ? AIPrompt.GET_DUMMY_PROMPT : newRequest,
                OpenAiChatOptions.builder()
                        .model(OpenAiApi.ChatModel.GPT_4_TURBO)
                        .maxTokens(100)
                        .build()));

        Dummy newDummy = Dummy.builder()
//                .member(member)
//                .isUserContent(isUserContent)
//                .request(AIPrompt.GET_DUMMY_PROMPT)
                .response(resp.getResult().getOutput().getText())
                .build();
        dummyRepository.save(newDummy);

//        member.getDummyList().add(newDummy);
        member.getInfo().updateReqCount();

        String text = resp.getResult().getOutput().getText();
        log.info("text result: {}", text);
        return text;
*/
        return null;
    }


    /**
     * 퀴즈를 만든 후 Redis 저장 및 캐시화
     *
     * @param userDetails (비영속성인 user 입니다.)
     * @param openQuizDate
     */
    public void openQuiz(Member userDetails, LocalDateTime openQuizDate) {
        Member member = memberRepository.findByEmail(userDetails.getEmail()).orElseThrow(() -> new UserHandler(ErrorCode.CANT_FIND_USER));

        if (!Objects.equals(member.getEmail(), "jijysun@naver.com")) {
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


    @Override
    public DummyResponseDTO.GetQuizInfoResponseDTO getQuiz(Member member) {

        Map<Object, Object> quiz = redisTemplate.opsForHash().entries("quiz");

        if (quiz.isEmpty()) {
            log.info("quiz is empty!"); // 사용자 별 이전 퀴즈 등수 확인
            Optional<MemberQuiz> userQuiz = memberQuizRepository.findLastestQuizByUserId(member.getId(), 1);

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


    @Override
    @Timed("quiz.solve.requests")
    @Transactional
    public void solveQuiz(Member userDetails, Long quizId, Integer answer) {

        // MySQL 비관적 락

        Member member = memberRepository.findByEmailFetchInfo(userDetails.getEmail()).orElseThrow(() -> new UserHandler(ErrorCode.CANT_FIND_USER));
        Quiz quiz = quizRepository.findQuizByIdForDecrease(quizId).orElseThrow(() -> new UserHandler(ErrorCode.WRONG_QUIZ));

        log.info("-- {}의 문제 풀이 작업 시작 --", member.getEmail());
        log.info("정답: {}, 제출 답안: {}", quiz.getAnswer(), answer);

        if (quiz == null) {
            log.warn("Wrong quiz!");
            throw new DummyHandler(ErrorCode.WRONG_QUIZ);
        }
        if (answer >= 5 || answer <= 0) {
            throw new DummyHandler(ErrorCode.WRONG_ANSWER);
        }

        // 중복 제출 방지
        if (redisTemplate.opsForHash().get("quiz", member.getId().toString()) != null) {
            log.warn("{} -> already submit", member.getEmail());
            throw new DummyHandler(ErrorCode.ALREADY_SUBMIT);
        }
        redisTemplate.opsForHash().put("quiz", member.getId().toString(), member.getEmail());

        if (!quiz.decreaseTicket()){ // ek
            log.warn("{} -> quiz has NO TICKET!",  member.getEmail());
            throw new DummyHandler(ErrorCode.TICKET_IS_DONE);
        }
        else{
            // 티켓 발급 로직
            member.getInfo().updateSubsExprDate(true, LocalDateTime.now().plusDays(3)); // 테스트 용 3일
        }

        log.info("-- {}의 문제 풀이 작업 종료 --", member.getEmail());
    }

}