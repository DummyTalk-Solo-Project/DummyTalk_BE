package DummyTalk.DummyTalk_BE.domain.service.dummy;

import DummyTalk.DummyTalk_BE.domain.converter.DummyConverter;
import DummyTalk.DummyTalk_BE.domain.dto.ChatCompletionResponseDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRespDTO;
import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.Member;
import DummyTalk.DummyTalk_BE.domain.entity.Quiz;
import DummyTalk.DummyTalk_BE.domain.entity.Rarity;
import DummyTalk.DummyTalk_BE.domain.entity.constant.AIPrompt;
import DummyTalk.DummyTalk_BE.domain.entity.constant.MemberRole;
import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.domain.entity.constant.RarityType;
import DummyTalk.DummyTalk_BE.domain.entity.document.DummyDocument;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberDummy;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.*;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.DummyHandler;
import DummyTalk.DummyTalk_BE.global.exception.handler.MemberHandler;
import DummyTalk.DummyTalk_BE.global.exception.handler.QuizHandler;
import DummyTalk.DummyTalk_BE.global.lock.DistributedLock;
import co.elastic.clients.elasticsearch._types.FieldValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
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

    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.api-key}")
    private String openAiKey;


    @Transactional
    public DummyRespDTO.GetDummyRespDTO getDummy(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("Member not found"));

        // 1. 천장 있는 지 조회
        String pityKey = "pity:" + memberId;
        Map<Object, Object> pity = redisTemplate.opsForHash().entries(pityKey);

        int rareStack = Integer.parseInt(pity.getOrDefault("RARE", "0").toString());
        int epicStack = Integer.parseInt(pity.getOrDefault("EPIC", "0").toString());
        int commonStack = Integer.parseInt(pity.getOrDefault("COMMON", "0").toString());

        Rarity selectedRarity = Rarity.defaultRarity();

        if (commonStack >= 10) {
            selectedRarity = rarityRepository.findByName(RarityType.valueOf("RARE")).orElseThrow(() -> new RuntimeException("Rarity not found"));
            log.info("[MemberService - GetDummy] - COMMON 천장 사용 -> RARE!");
            updatePityStack(pityKey, "COMMON", true);
        }
        else if (rareStack >= 10) {
            selectedRarity = rarityRepository.findByName(RarityType.valueOf("EPIC")).orElseThrow(() -> new RuntimeException("Rarity not found"));
            log.info("[MemberService - GetDummy] - RARE 천장 사용 -> EPIC!");
            updatePityStack(pityKey, "RARE", true);
        }
        else if (epicStack >= 10) {
            selectedRarity = rarityRepository.findByName(RarityType.valueOf("SPECIAL")).orElseThrow(() -> new RuntimeException("Rarity not found"));
            log.info("[MemberService - GetDummy] - EPIC 천장 사용 -> SPECIAL!");
            updatePityStack(pityKey, "EPIC", true);
        }
        else{
            // 2. 천장 없는 경우 확률에 의해 조회.
            selectedRarity = getRandomRarity();
            log.info("[MemberService - getDummy] selectedRarity: " + selectedRarity.getName().toString());
            updatePityStack(pityKey, selectedRarity.getName().toString(), false);
        }

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

        return DummyRespDTO.GetDummyRespDTO.builder()
                .dummyId(dummy.getId())
                .title(dummy.getTitle())
                .content(dummy.getContent())
                .rarityName(dummy.getRarity().getName().toString())
                .build();
    }

    private void updatePityStack(String key, String wonRarity, Boolean isPity) {
        if (isPity) {
            redisTemplate.opsForHash().put(key, wonRarity, "0");
            switch (wonRarity) {
                case ("COMMON"):
                    redisTemplate.opsForHash().increment(key, "RARE", 1);
                    break;
                case ("RARE"):
                    redisTemplate.opsForHash().increment(key, "EPIC", 1);
                    break;
                default:
                    break;
            }
        }
        else if (!wonRarity.equals("SPECIAL")) {
            redisTemplate.opsForHash().increment(key, wonRarity, 1);
            log.info("[MemberService - updatePityStack] - {} 스택 증가 = {}", wonRarity, redisTemplate.opsForHash().get(key, wonRarity));
        }

/*        if (wonRarity.equals("EPIC") || ) {
            // 대박 등급 당첨 시 EPIC 만 초기화
            redisTemplate.opsForHash().put(key, "EPIC", "0");
        } else if (wonRarity.equals("RARE")) {
            // RARE 당첨 시 RARE 천장만 초기화, EPIC은 계속 누적
            redisTemplate.opsForHash().put(key, "RARE", "0");
            redisTemplate.opsForHash().increment(key, "EPIC", 1);
        } else {
            // COMMON 당첨 시 RARE 천장 스택 +1
            redisTemplate.opsForHash().increment(key, "RARE", 1);
        }*/
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
        for (Rarity r : rarityList) {
            cumulative += r.getProbability();
            if (pivot <= cumulative) {
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
        redisTemplate.opsForValue().set("quiz", savedQuiz.getId(), 10, TimeUnit.MINUTES);


        // 4. openQuiz scheduling


        return savedQuiz;
    }


    public DummyRespDTO.GetQuizInfoResponseDTO getQuiz(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));

        Long quizId = Long.valueOf(redisTemplate.opsForValue().get("quiz").toString());
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new QuizHandler(ErrorCode.WRONG_QUIZ));

        return DummyRespDTO.GetQuizInfoResponseDTO.createDTO(quiz);
    }


    // 이전 기본 로직 메소드
    @Timed("quiz.solve.requests")
    public void solveQuiz(String email, Long quizId, Integer answer) {
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
