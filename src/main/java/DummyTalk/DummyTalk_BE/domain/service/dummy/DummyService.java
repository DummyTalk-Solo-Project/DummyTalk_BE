package DummyTalk.DummyTalk_BE.domain.service.dummy;

import DummyTalk.DummyTalk_BE.domain.converter.DummyConverter;
import DummyTalk.DummyTalk_BE.domain.dto.ChatCompletionResponseDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRespDTO;
import DummyTalk.DummyTalk_BE.domain.entity.*;
import DummyTalk.DummyTalk_BE.domain.entity.constant.AIPrompt;
import DummyTalk.DummyTalk_BE.domain.entity.constant.MemberRole;
import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.domain.entity.constant.RarityType;
import DummyTalk.DummyTalk_BE.domain.entity.document.DummyDocument;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberDummy;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberQuiz;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.*;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.DummyHandler;
import DummyTalk.DummyTalk_BE.global.exception.handler.MemberHandler;
import DummyTalk.DummyTalk_BE.global.exception.handler.QuizHandler;
import DummyTalk.DummyTalk_BE.global.event.DummyViewedEvent;
import DummyTalk.DummyTalk_BE.global.lock.DistributedLock;
import DummyTalk.DummyTalk_BE.global.scheduler.QuizScheduler;
import co.elastic.clients.elasticsearch._types.FieldValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DummyService {


    // 3. 동시성 관련 로직 or @Async 추가
    // Security 잠시 빼기

    private final MemberRepository memberRepository;
    private final RarityRepository rarityRepository;
    private final DummyRepository dummyRepository;
    private final MemberDummyRepository memberDummyRepository;
    private final QuizRepository quizRepository;

    private final ElasticsearchOperations elasticsearchOperations;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final QuizScheduler quizScheduler;

    private final ObjectMapper objectMapper;
    private final MemberQuizRepository memberQuizRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${spring.ai.openai.api-key}")
    private String openAiKey;


    @Transactional
    public DummyRespDTO.GetDummyRespDTO getDummy(Long memberId) {
        Member member = memberRepository.findByIdFetchJoinInfo(memberId).orElseThrow(() -> new RuntimeException("Member not found"));
        Info info = member.getInfo();

        if (info.getReqCount() >= 20){ // 일단 하루 20번으로 설정.
            throw new DummyHandler(ErrorCode.USED_ALL_CHANCES);
        }

        // 1. 천장 있는 지 조회
        String pityKey = "pity:" + memberId;
        Map<Object, Object> pity = redisTemplate.opsForHash().entries(pityKey);

        // 의미가 헷갈린다.
        int currentCommonStack = Integer.parseInt(pity.getOrDefault("COMMON", "0").toString());
        int currentRareStack = Integer.parseInt(pity.getOrDefault("RARE", "0").toString());
        int currentEpicStack = Integer.parseInt(pity.getOrDefault("EPIC", "0").toString());

        Boolean isPityTriggered = false;

        Rarity selectedRarity = Rarity.defaultRarity();

        if (currentCommonStack >= 10) { // COMMON -> RARE
            selectedRarity = rarityRepository.findByName(RarityType.valueOf("RARE")).orElseThrow(() -> new RuntimeException("Rarity not found"));
            log.info("[MemberService - GetDummy] - COMMON 천장 사용 -> RARE!");
            isPityTriggered=true;
        }
        else if (currentRareStack >= 10) { // RARE -> EPIC
            selectedRarity = rarityRepository.findByName(RarityType.valueOf("EPIC")).orElseThrow(() -> new RuntimeException("Rarity not found"));
            log.info("[MemberService - GetDummy] - RARE 천장 사용 -> EPIC!");
            isPityTriggered=true;
        }
        else if (currentEpicStack >= 10) { // EPIC -> SPECIAL
            selectedRarity = rarityRepository.findByName(RarityType.valueOf("SPECIAL")).orElseThrow(() -> new RuntimeException("Rarity not found"));
            log.info("[MemberService - GetDummy] - EPIC 천장 사용 -> SPECIAL!");
            isPityTriggered=true;
        }
        else{
            // 2. 천장 없는 경우 확률에 의해 조회.
            selectedRarity = getRandomRarity();
            log.info("[MemberService - getDummy] 랜덤 뽑기: " + selectedRarity.getName());
        }

        // 스택 update, 다음 뽑기 천장 예정 여부 반환
        Boolean isNextPityTriggered = updatePityStack(pityKey, selectedRarity.getName(), isPityTriggered);

        // {dummy:등급} set에 저장되어 있는 id 중 하나 랜덤으로 긁어옴
        Object result = redisTemplate.opsForSet().randomMember("dummy:" + selectedRarity.getName());
        if (result == null) {
            throw new RuntimeException("No dummy found in Redis for rarity: " + selectedRarity.getName());
        }
        Long randomDummyId = Long.valueOf(result.toString());

        // 한 번에 찾기
        Dummy dummy = dummyRepository.findByIdWithRarity(randomDummyId).orElseThrow(() -> new RuntimeException("Dummy not found"));

        // 조회 기록으로 저장
        memberDummyRepository.save(MemberDummy.generateMemberDummy(member, dummy));
        redisTemplate.opsForSet().add("member:"+memberId+":dummy", dummy.getId());
        
        long totalDummyCount = memberDummyRepository.countByMember_Id(memberId); // 뱃지 체크용 누적 횟수 (save 직후 동일 트랜잭션에서!)

        info.updateReqCount();

        // 비동기 뱃지 이벤트 발행 (트랜잭션 커밋 후 MailExecutor 풀에서 처리)
        eventPublisher.publishEvent(new DummyViewedEvent(memberId, dummy.getRarity().getName().toString(), isPityTriggered, totalDummyCount));

        DummyRespDTO.GetDummyRespDTO dto = DummyRespDTO.GetDummyRespDTO.builder()
                .dummyId(dummy.getId())
                .title(dummy.getTitle())
                .content(dummy.getContent())
                .rarityName(dummy.getRarity().getName().toString())
                .isPityTriggered(isPityTriggered)
                .isNextPityTriggered(isNextPityTriggered)
                .remainingCount(20 - info.getReqCount())
                .build();
        log.info("[MemberService - getDummy] dto = " + selectedRarity.getName().toString());
        return dto;
    }

    // 스택 업데이트 후 다음 뽑기가 천장 확정인지 반환 (increment 반환값 >= 10)
    private Boolean updatePityStack(String key, RarityType wonRarity, Boolean isPityTriggered) {
        if (isPityTriggered) {
            if (wonRarity == RarityType.SPECIAL) {
                // SPECIAL은 최상위 등급, 다음 천장 없음
                redisTemplate.opsForHash().put(key, "EPIC", "0");
                log.info("[MemberService - updatePityStack] - EPIC 천장! => SPECIAL");
                return false;
            }
            else if (wonRarity == RarityType.EPIC) {
                redisTemplate.opsForHash().put(key, "RARE", "0");
                Long newEpic = redisTemplate.opsForHash().increment(key, "EPIC", 1);
                log.info("[MemberService - updatePityStack] - RARE 천장! => EPIC, newEpicStack={}", newEpic);
                return newEpic >= 10;
            }
            else if (wonRarity == RarityType.RARE) {
                redisTemplate.opsForHash().put(key, "COMMON", "0");
                Long newRare = redisTemplate.opsForHash().increment(key, "RARE", 1);
                log.info("[MemberService - updatePityStack] - COMMON 천장! => RARE, newRareStack={}", newRare);
                return newRare >= 10;
            }
        }
        else {
            if (wonRarity == RarityType.COMMON) {
                Long newCommon = redisTemplate.opsForHash().increment(key, "COMMON", 1);
                log.info("[MemberService - updatePityStack] - COMMON 스택 증가 = {}", newCommon);
                return newCommon >= 10;
            }
            else if (wonRarity == RarityType.RARE) {
                Long newRare = redisTemplate.opsForHash().increment(key, "RARE", 1);
                log.info("[MemberService - updatePityStack] - RARE 스택 증가 = {}", newRare);
                return newRare >= 10;
            }
            else if (wonRarity == RarityType.EPIC) {
                Long newEpic = redisTemplate.opsForHash().increment(key, "EPIC", 1);
                log.info("[MemberService - updatePityStack] - EPIC 스택 증가 = {}", newEpic);
                return newEpic >= 10;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<DummyRespDTO.GetMyDummyDTO> getMyDummyList (Long memberId, int page){
        Set<Object> members = redisTemplate.opsForSet().members("member:" + memberId.toString() + ":dummy");
        List<Long> dummyIdList = members.stream()
                .map(id -> Long.valueOf(id.toString()))
                .toList();

        log.info("[MemberService - GetMyDummyList] - memberId:{} -> dummyIdList.size: {}",  memberId, dummyIdList.size());

        return DummyConverter.toGetMyDummyListDTO(memberDummyRepository.findAllByDummyIdList(dummyIdList, PageRequest.of(page, 10)).getContent());
    }

    @Transactional(readOnly = true)
    public List<DummyRespDTO.GetMyDummyDTO> getMyDummyListWithKeyword(Long memberId, String keyword, Integer page){

        Set<Object> members = redisTemplate.opsForSet().members("member:" + memberId + ":dummy");

        if (members.isEmpty()){
            throw new RuntimeException("No dummy found in Redis for member id: " + memberId);
        }

        List<FieldValue> dummyIdList = members.stream()
                .map(id -> FieldValue.of(id.toString()))
                .toList();

        // NativeQuery
        NativeQuery nq = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .multiMatch(mm -> mm
                                                .fields("title^2", "content") // 내용 보다는 제목에 가중치
                                                .query(keyword)
                                        )
                                )
                                .filter(f -> f
                                        .terms(t -> t
                                                .field("_id")
                                                .terms(v -> v
                                                        .value(dummyIdList)))
                                )
                        )
                )
                .withPageable(PageRequest.of(page, 10))
                .build();

        List<DummyDocument> dummyDocumentList = elasticsearchOperations.search(nq, DummyDocument.class)
                .stream().map(SearchHit::getContent).toList();

        return DummyConverter.toGetMyDummyDListTO(dummyDocumentList);
    }

    @Transactional
    public String GetDummyDateForNormal(String email, DummyRequestDTO.RequestInfoDTO requestInfoDTO) {
        return null;
    }


    @Transactional(readOnly = true)
    public Rarity getRandomRarity (){
        List<Rarity> rarityList = rarityRepository.findAll(); // 최대 4개.
        double pivot = Math.random() * 100;
        double cumulative = 0;
        for (int i = 0; i < rarityList.size(); i++) {
            Rarity r = rarityList.get(i);
            cumulative += r.getProbability();

            // 당첨 조건 이거나, 마지막 요소인 경우 강제로
            if (pivot <= cumulative || i == rarityList.size() - 1) {
                return r;
            }
        }
        return null;
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
        Rarity selectedRarity = getRandomRarity();

        Object result = redisTemplate.opsForSet().randomMember("dummy:" + selectedRarity.getName());
        if (result == null) {
            throw new RuntimeException("No dummy found in Redis for rarity: " + selectedRarity.getName());
        }

        Dummy randomDummy = dummyRepository.findById(Long.valueOf(result.toString())).orElseThrow(() -> new RuntimeException("No dummy found in Redis for rarity: " + result));
        log.info("[DummyService - openQuiz] - randomDummy.id: {}",randomDummy.getId());

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

        log.info("[DummyService - openQuiz] - resp.toString(): {}", resp);

        // 3. 해당 시간에 Quiz 생성 & return
        Quiz savedQuiz = quizRepository.save(Quiz.createNewQuiz(resp.getTitle(), resp.getAnswerList(), resp.getAnswer(), resp.getDescription(), 10, openQuizDate));
        redisTemplate.opsForValue().set("quiz", savedQuiz.getId(), 3, TimeUnit.MINUTES);


        // 4. openQuiz scheduling
        int startTime = openQuizDate.getMinute() - LocalDateTime.now().getMinute() ;
        Instant later = Instant.now().plus(startTime, ChronoUnit.MINUTES);
        taskScheduler.schedule (quizScheduler.controlQuiz(savedQuiz.getId(), QuizStatus.OPEN), later);

        Instant closeTime = Instant.now().plus(startTime+1, ChronoUnit.MINUTES);
        taskScheduler.schedule (quizScheduler.controlQuiz(savedQuiz.getId(), QuizStatus.CLOSE), closeTime);


        return savedQuiz;
    }


    public DummyRespDTO.GetQuizInfoResponseDTO getQuiz(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));

        Object o = redisTemplate.opsForValue().get("quiz");
        if (o == null) {
            throw new QuizHandler(ErrorCode.QUIZ_NOT_OPEN);
        }
        Long quizId = Long.valueOf(o.toString());
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new QuizHandler(ErrorCode.WRONG_QUIZ));

        return DummyRespDTO.GetQuizInfoResponseDTO.createDTO(quiz);
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
     * 퀴즈 풀이 메서드 (비관적 락 적용)
     *
     * 동시성 전략: PESSIMISTIC_WRITE (SELECT FOR UPDATE)
     * - 동일 quizId 요청을 DB 레벨에서 직렬화하여 ticket 감소의 Lost Update 방지
     * - HikariCP 풀 고갈 등 문제 발생 시 Redisson 분산락(@DistributedLock)으로 전환 예정
     * - PESSIMISTIC_WRITE 동안 하는 작업이 너무 많으므로 개선 필요.
     */
    @Transactional
    public Boolean solveQuiz(Long memberId, Long quizId, Integer answer) {

        // 중복 제출 방지
        if (memberQuizRepository.existsByMemberIdAndQuizId(memberId, quizId)) {
            throw new QuizHandler(ErrorCode.ALREADY_SUBMIT);
        }

        // 주의! 없을 경우 save() 부분에서 EntityNotFoundException 발생
        Member member = memberRepository.getReferenceById(memberId);

        // Quiz 조회 with PESSIMISTIC_WRITE - ticket 감소 전 배타적 잠금 획득
        Quiz quiz = quizRepository.findQuizByIdForDecrease(quizId)
                .orElseThrow(() -> new QuizHandler(ErrorCode.WRONG_QUIZ));

        if (quiz.getStatus().equals(QuizStatus.NOT_OPEN)) {
            throw new QuizHandler(ErrorCode.QUIZ_NOT_OPEN);
        }

        if (quiz.getStatus().equals(QuizStatus.CLOSE)) {
            throw new QuizHandler(ErrorCode.QUIZ_IS_CLOSED);
        }

        // 틀린 답안
        if ((answer < 1 || answer > quiz.getAnswerList().size()) || ( !Objects.equals(quiz.getAnswer(), answer))) {
            throw new QuizHandler(ErrorCode.WRONG_ANSWER);
        }

        // PESSIMISTIC_WRITE 락 내에서 실행되므로 동시 요청 간 Race Condition 없음
        if (!quiz.decreaseTicket()) {
            throw new QuizHandler(ErrorCode.TICKET_IS_DONE);
        }

        // memberGrade은 deprecated. 실제 등수는 아래 Redis list 순서로 집계 예정
        memberQuizRepository.save(MemberQuiz.generateMemberQuiz(member, quiz, 1, answer));

        // 인덱스 == (등수 - 1)
        // 퀴즈 종료 후 순위 정산(등수 계산, 보상 차등 지급 등)에 활용 예정
        redisTemplate.opsForList().rightPush("quiz:" + quizId, memberId + ":" + answer);

        return true;
    }

    /**
     * Synchronized를 통한 동시성 해결 메소드
     *
     */
    @Timed("quiz.solve.requests")
    public synchronized void solveQuizVer2(String email, Integer answer) {

    }

    /**
     * Redisson 분산 락 적용
     *
     */
    @Timed("quiz.solve.requests")
    @DistributedLock(key = "'quiz_lock'")
    public void solveQuizVer3(String email, Integer answer) {

    }


    /**
     * MySQL 단 락 적용
     * Quiz 엔티티의 ticket 에 대한 동시성 해결 메소드
     * */
    @Timed("quiz.solve.requests")
    @Transactional
    public void solveQuizVer4(DummyRequestDTO.SolveQuizReqDTO dto) {

    }

}
