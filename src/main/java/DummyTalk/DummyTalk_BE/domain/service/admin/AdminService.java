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
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.*;
import java.util.List;
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
    // Stage 4: TaskScheduler 인터페이스로 교체 — SchedulerConfig의 VTTaskScheduler(@Primary) 주입됨
    private final TaskScheduler taskScheduler;
    private final QuizScheduler quizScheduler;
    private final RedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;
    private final DummyService dummyService;
    private final RarityRepository rarityRepository;
    private final InfoRepository infoRepository;

    // ─── K6 부하 테스트 지원 API 게이트/헤더 값 ───
    // test.load-users=false(운영)면 ADMIN이어도 LOAD_TEST_DISABLED — TestMemberDataLoader와 같은 스위치로 묶어
    // 측정 서버에서만 켜지고 Oracle 상시 배포에서는 프로퍼티만 내리면 자동 비활성
    @Value("${test.load-users:false}")
    private boolean loadTestEnabled;
    @Value("${concurrency.getDummy-version:3}")
    private int getDummyVersion;
    @Value("${concurrency.interceptor-enabled:true}")
    private boolean interceptorEnabled;
    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int hikariPoolSize;
    @Value("${spring.threads.virtual.enabled:false}")
    private boolean virtualThreadsEnabled;
    @Value("${concurrency.async-virtual-threads:true}")
    private boolean asyncVirtualThreadsEnabled;

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

        // Stage 4 VT 전환: SimpleAsyncTaskScheduler는 Pool이 없음 (태스크당 VT 생성)
        // → Pool 모니터링 지표는 의미 상실, -1 고정값으로 응답 (FE에서 "VT 모드" 표시용)
        return DummyRespDTO.CheckQuizDTO.builder()
                .activeCount(-1)
                .poolSize(-1)
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
        Rarity selectedRarity = rarityRepository.findByName(dummyService.getRandomRarityType()).orElseThrow(()-> new AdminHandler(ErrorCode.WRONG_RARITY));

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
    // ===================== K6 부하 테스트 지원 =====================

    // ADMIN 권한 + test.load-users 2중 게이트 (기존 ADMIN 체크 패턴 재사용)
    private void checkLoadTestAccess(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getRole().equals(MemberRole.MEMBER)) {
            throw new MemberHandler(ErrorCode.AUTH_FORBIDDEN);
        }
        if (!loadTestEnabled) {
            throw new AdminHandler(ErrorCode.LOAD_TEST_DISABLED);
        }
    }

    /**
     * 회차 사이 테스트 유저 초기화 — k6 setup() 이 호출.
     * reqCount=0 + isSubscribe=true(40회 한도). 이전엔 EC2에서 수동 SQL(UPDATE info SET req_count=0)로 하던 것.
     * @return 갱신된 테스트 유저 수 (= 시딩된 유저 수와 일치해야 정상)
     */
    @Transactional
    public int resetLoadTestUsers(Long memberId) {
        checkLoadTestAccess(memberId);
        int updated = infoRepository.resetLoadTestUsers();
        log.warn("[AdminService - resetLoadTestUsers()] - 부하 테스트 유저 초기화 | updated={}", updated);
        return updated;
    }

    /**
     * 회차 헤더/판정용 스냅샷 — k6 setup()·teardown() 이 각각 호출.
     * reqCountSum 의 전후 델타 = DB 가 실제로 반영한 뽑기 수 → k6 success_200 과 비교해 Lost Update 자동 판정.
     * 서버 설정값은 결과 파일에 기록되어 "어느 설정의 회차였나"를 재현 가능하게 함.
     */
    @Transactional(readOnly = true)
    public AdminRespDTO.LoadTestStateDTO getLoadTestState(Long memberId) {
        checkLoadTestAccess(memberId);
        return AdminRespDTO.LoadTestStateDTO.builder()
                .testUserCount(infoRepository.countLoadTestUsers())
                .reqCountSum(infoRepository.sumReqCountOfLoadTestUsers())
                .getDummyVersion(getDummyVersion)
                .interceptorEnabled(interceptorEnabled)
                .hikariPoolSize(hikariPoolSize)
                .virtualThreads(virtualThreadsEnabled)
                .asyncVirtualThreads(asyncVirtualThreadsEnabled)
                .build();
    }
}
