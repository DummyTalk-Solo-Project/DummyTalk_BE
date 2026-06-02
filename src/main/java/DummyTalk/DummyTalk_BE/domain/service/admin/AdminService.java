package DummyTalk.DummyTalk_BE.domain.service.admin;

import DummyTalk.DummyTalk_BE.domain.dto.ChatCompletionResponseDTO;
import DummyTalk.DummyTalk_BE.domain.dto.admin.AdminRespDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRespDTO;
import DummyTalk.DummyTalk_BE.domain.entity.*;
import DummyTalk.DummyTalk_BE.domain.entity.constant.AIPrompt;
import DummyTalk.DummyTalk_BE.domain.entity.constant.MemberRole;
import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.*;
import DummyTalk.DummyTalk_BE.domain.service.dummy.DummyService;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.AdminHandler;
import DummyTalk.DummyTalk_BE.global.exception.handler.DummyHandler;
import DummyTalk.DummyTalk_BE.global.exception.handler.MemberHandler;
import DummyTalk.DummyTalk_BE.global.exception.handler.QuizHandler;
import DummyTalk.DummyTalk_BE.global.scheduler.QuizScheduler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final DailySettlementRepository dailySettlementRepository;
    private final MemberRepository memberRepository;
    private final DummyRepository dummyRepository;
    private final QuizRepository  quizRepository;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final QuizScheduler quizScheduler;
    private final RedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;
    private final DummyService dummyService;
    private final RarityRepository rarityRepository;

    @Value("${spring.ai.openai.api-key}")
    private String openAiKey;

    // 특정 날짜의 정산 데이터 단건 조회
    // AdminTask 가 매일 00:30 에 전날치를 저장하므로 오늘 날짜는 조회 불가
    @Transactional(readOnly = true)
    public AdminRespDTO.DailySettlementRespDTO getDailySettlement(Long memberId, LocalDate date) {

        // 기존 서비스 계층과 동일한 Admin 권한 체크 패턴
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getRole().equals(MemberRole.MEMBER)) {
            throw new MemberHandler(ErrorCode.AUTH_FORBIDDEN);
        }

        DailySettlement settlement = dailySettlementRepository.findBySettlementDate(date)
                .orElseThrow(() -> new AdminHandler(ErrorCode.SETTLEMENT_NOT_FOUND));

        log.info("[AdminService - getDailySettlement()] - 정산 조회 | 날짜={}", date);
        return AdminRespDTO.DailySettlementRespDTO.from(settlement);
    }

    // 기간별 정산 목록 조회
    @Transactional(readOnly = true)
    public List<AdminRespDTO.DailySettlementRespDTO> getSettlementRange(Long memberId, LocalDate from, LocalDate to) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getRole().equals(MemberRole.MEMBER)) {
            throw new MemberHandler(ErrorCode.AUTH_FORBIDDEN);
        }
        log.info("[AdminService - getSettlementRange()] - 기간별 정산 조회 | from={}, to={}", from, to);
        return dailySettlementRepository.findBySettlementDateBetweenOrderBySettlementDateAsc(from, to)
                .stream()
                .map(AdminRespDTO.DailySettlementRespDTO::from)
                .collect(Collectors.toList());
    }

    // 최근 N일 정산 목록 조회 — AdminTask 기준 어제까지 유효
    @Transactional(readOnly = true)
    public List<AdminRespDTO.DailySettlementRespDTO> getLatestSettlements(Long memberId, Integer days) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getRole().equals(MemberRole.MEMBER)) {
            throw new MemberHandler(ErrorCode.AUTH_FORBIDDEN);
        }
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate from = yesterday.minusDays(days - 1);
        log.info("[AdminService - getLatestSettlements()] - 최근 {}일 정산 조회 | from={}, to={}", days, from, yesterday);
        return dailySettlementRepository.findBySettlementDateBetweenOrderBySettlementDateAsc(from, yesterday)
                .stream()
                .map(AdminRespDTO.DailySettlementRespDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * Admin 전용
     * */
    public DummyRespDTO.CheckQuizDTO checkQuiz (Long memberId){
        // NotAdmin? reject!
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getRole().equals(MemberRole.MEMBER)){
            throw new MemberHandler(ErrorCode.AUTH_FORBIDDEN);
        }

        return DummyRespDTO.CheckQuizDTO.builder()
                .activeCount(taskScheduler.getActiveCount())
                .poolSize(taskScheduler.getPoolSize())
                .build();
    }

    /**
     * 퀴즈를 만든 후 Redis 저장 및 캐시화
     *
     * @param memberId
     * @param openQuizDate
     * @return
     */
    @Transactional
    public Quiz openQuiz(Long memberId, LocalDateTime openQuizDate) {

        // NotAdmin? reject!
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getRole().equals(MemberRole.MEMBER)){
            throw new MemberHandler(ErrorCode.AUTH_FORBIDDEN);
        }

        // 1. Special 제외 랜덤 문제 조회
        Rarity selectedRarity = dummyService.getRandomRarity();

        Object result = redisTemplate.opsForSet().randomMember("dummy:" + selectedRarity.getName());
        if (result == null) {
            throw new DummyHandler(ErrorCode.DUMMY_WITH_RARITY_NOT_FOUND);
        }

        Dummy randomDummy = dummyRepository.findById(Long.valueOf(result.toString())).orElseThrow(() -> new DummyHandler(ErrorCode.DUMMY_WITH_ID_NOT_FOUND));
        log.info("[DummyService - openQuiz()] - randomDummy.id: {}", randomDummy.getId());

        // 2. 해당 문제를 통해 OpenAiAPI -> 문제를 만들어줘
        DummyRequestDTO.GetDummyQuizDTO dto = DummyRequestDTO.GetDummyQuizDTO.builder()
                .model("gpt-4o-mini")
                .messages(List.of(new DummyRequestDTO.Message(
                        "user",
                        AIPrompt.generateNewQuizPrompt(randomDummy))))
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

        DummyRespDTO.GetQuizFromAIResponseDTO resp;
        try {
            resp = objectMapper.readValue(text, DummyRespDTO.GetQuizFromAIResponseDTO.class);
        } catch (JsonProcessingException e) {
            throw new DummyHandler(ErrorCode.AI_PARSING_ERROR);
        }

        log.info("[DummyService - openQuiz()] - resp: {}", resp);

        // 3. 해당 시간에 Quiz 생성 & return
        Quiz savedQuiz = quizRepository.save(Quiz.createNewQuiz(resp.getTitle(), resp.getAnswerList(), resp.getAnswer(), resp.getDescription(), 10, openQuizDate));

        // 4. openQuiz scheduling
        // LocalDateTime → Instant  (시스템 타임존 기준): getMinute() 차분 방식의 날짜 + 시간 무시 버그 수정
        Instant now = Instant.now();
        Instant openInstant  = openQuizDate.atZone(ZoneId.systemDefault()).toInstant();
        Instant closeInstant = savedQuiz.getEndTime().atZone(ZoneId.systemDefault()).toInstant(); // Quiz.endTime = startTime+5min

        // 과거 시간 검증 — 이미 지난 시간으로 스케줄하면 즉시 실행되므로 차단
        if (openInstant.isBefore(now)) {
            throw new QuizHandler(ErrorCode.QUIZ_INVALID_OPEN_TIME);
        }

        redisTemplate.opsForValue().set("quiz", savedQuiz.getId(), Duration.between(now, closeInstant).getSeconds(), TimeUnit.SECONDS); // Redis 키 TTL = 퀴즈가 닫히는 시점까지 유지 (동적으로)

        taskScheduler.schedule(quizScheduler.controlQuiz(savedQuiz.getId(), QuizStatus.OPEN),  openInstant);
        taskScheduler.schedule(quizScheduler.controlQuiz(savedQuiz.getId(), QuizStatus.CLOSE), closeInstant);


        return savedQuiz;
    }
}