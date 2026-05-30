package DummyTalk.DummyTalk_BE.domain.service.dummy;

import DummyTalk.DummyTalk_BE.domain.converter.DummyConverter;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRespDTO;
import DummyTalk.DummyTalk_BE.domain.entity.*;
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
import co.elastic.clients.elasticsearch._types.FieldValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DummyService {


    private final MemberRepository memberRepository;
    private final RarityRepository rarityRepository;
    private final DummyRepository dummyRepository;
    private final MemberDummyRepository memberDummyRepository;
    private final QuizRepository quizRepository;

    private final ElasticsearchOperations elasticsearchOperations;
    private final RedisTemplate<String, Object> redisTemplate;

    private final MemberQuizRepository memberQuizRepository;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * 트랜잭션의 동기화가 이루어져있지 않음
     * PostgreSQL -> 트랜잭션 수행 중 연산 오류로 인한 롤백 -> Redis는....?
     * @return
     */
    @Transactional
    public DummyRespDTO.GetDummyRespDTO getDummy(Long memberId) {
        ///  READ - 따닥 급의 동일 사용자, n번의 요청
        Member member = memberRepository.findByIdFetchJoinInfo(memberId).orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));
        Info info = member.getInfo();

        ///  MODIFY - n개의 스레드 통과
        if ((info.getIsSubscribe() && info.getReqCount() >= 40) || (!info.getIsSubscribe() && info.getReqCount() >= 20)){
            throw new DummyHandler(ErrorCode.USED_ALL_CHANCES); // 구독자는 40번, 미구독자는 20번
        }

        // 1. 천장 있는 지 조회
        String pityKey = "pity:" + memberId;
        Map<Object, Object> pity = redisTemplate.opsForHash().entries(pityKey);

        int currentCommonStack = Integer.parseInt(pity.getOrDefault("COMMON", "0").toString());
        int currentRareStack = Integer.parseInt(pity.getOrDefault("RARE", "0").toString());
        int currentEpicStack = Integer.parseInt(pity.getOrDefault("EPIC", "0").toString());

        Boolean isPityTriggered = false;

        Rarity selectedRarity = Rarity.defaultRarity();

        if (currentCommonStack >= 10) { // COMMON -> RARE
            selectedRarity = rarityRepository.findByName(RarityType.valueOf("RARE")).orElseThrow(() -> new DummyHandler(ErrorCode.WRONG_RARITY));
            log.info("[DummyService - getDummy()] - COMMON 천장 사용 -> RARE!");
            isPityTriggered=true;
        }
        else if (currentRareStack >= 10) { // RARE -> EPIC
            selectedRarity = rarityRepository.findByName(RarityType.valueOf("EPIC")).orElseThrow(() -> new DummyHandler(ErrorCode.WRONG_RARITY));
            log.info("[DummyService - getDummy()] - RARE 천장 사용 -> EPIC!");
            isPityTriggered=true;
        }
        else if (currentEpicStack >= 10) { // EPIC -> SPECIAL
            selectedRarity = rarityRepository.findByName(RarityType.valueOf("SPECIAL")).orElseThrow(() -> new DummyHandler(ErrorCode.WRONG_RARITY));
            log.info("[DummyService - getDummy()] - EPIC 천장 사용 -> SPECIAL!");
            isPityTriggered=true;
        }
        else{
            // 2. 천장 없는 경우 확률에 의해 조회.
            selectedRarity = getRandomRarity();
            log.info("[DummyService - getDummy()] - 랜덤 뽑기: {}", selectedRarity.getName());
        }

        // 스택 update, 다음 뽑기 천장 예정 여부 반환
        Boolean isNextPityTriggered = updatePityStack(pityKey, selectedRarity.getName(), isPityTriggered);

        // {dummy:등급} set에 저장되어 있는 id 중 하나 랜덤으로 긁어옴
        Object result = redisTemplate.opsForSet().randomMember("dummy:" + selectedRarity.getName());
        if (result == null) {
            throw new DummyHandler(ErrorCode.WRONG_RARITY);
        }
        Long randomDummyId = Long.valueOf(result.toString());

        // 한 번에 찾기
        Dummy dummy = dummyRepository.findByIdWithRarity(randomDummyId).orElseThrow(() -> new DummyHandler(ErrorCode.WRONG_DUMMY));

        // 조회 기록으로 저장
        memberDummyRepository.save(MemberDummy.generateMemberDummy(member, dummy));
        redisTemplate.opsForSet().add("member:"+memberId+":dummy", dummy.getId());

        /// 기록이 많은 사용자의 경우 O(N) 이상의 시간이 걸릴 수 있음.
        long totalDummyCount = memberDummyRepository.countByMember_Id(memberId); // 뱃지 체크용 누적 횟수 (save 직후 동일 트랜잭션에서!)


        ///  WRITE - Concurrency Problem!!!
        info.updateReqCount();

        // 비동기 뱃지 이벤트 발행 (트랜잭션 커밋 후 MailExecutor 풀에서 처리)
        /*
        * 실행시점: 트랜잭션 Commit 전 = MemmberDummy에 대한 기록이 존재하지 않음!!!!
        * */
        eventPublisher.publishEvent(new DummyViewedEvent(memberId, dummy.getRarity().getName().toString(), isPityTriggered, totalDummyCount));

        DummyRespDTO.GetDummyRespDTO dto = DummyRespDTO.GetDummyRespDTO.builder()
                .dummyId(dummy.getId())
                .title(dummy.getTitle())
                .content(dummy.getContent())
                .rarityName(dummy.getRarity().getName().toString())
                .isPityTriggered(isPityTriggered)
                .isNextPityTriggered(isNextPityTriggered)
                .remainingCount((Boolean.TRUE.equals(info.getIsSubscribe()) ? 40 : 20) - info.getReqCount())
                .build();
        log.info("[DummyService - getDummy()] - selectedRarity: {}", selectedRarity.getName());
        return dto;
    }

    // 스택 업데이트 후 다음 뽑기가 천장 확정인지 반환 (increment 반환값 >= 10)
    private Boolean updatePityStack(String key, RarityType wonRarity, Boolean isPityTriggered) {
        if (isPityTriggered) {
            if (wonRarity == RarityType.SPECIAL) {
                // SPECIAL은 최상위 등급, 다음 천장 없음
                redisTemplate.opsForHash().put(key, "EPIC", "0");
                log.info("[DummyService - updatePityStack()] - EPIC 천장! => SPECIAL");
                return false;
            }
            else if (wonRarity == RarityType.EPIC) {
                redisTemplate.opsForHash().put(key, "RARE", "0");
                Long newEpic = redisTemplate.opsForHash().increment(key, "EPIC", 1);
                log.info("[DummyService - updatePityStack()] - RARE 천장! => EPIC, newEpicStack={}", newEpic);
                return newEpic >= 10;
            }
            else if (wonRarity == RarityType.RARE) {
                redisTemplate.opsForHash().put(key, "COMMON", "0");
                Long newRare = redisTemplate.opsForHash().increment(key, "RARE", 1);
                log.info("[DummyService - updatePityStack()] - COMMON 천장! => RARE, newRareStack={}", newRare);
                return newRare >= 10;
            }
        }
        else {
            if (wonRarity == RarityType.COMMON) {
                Long newCommon = redisTemplate.opsForHash().increment(key, "COMMON", 1);
                log.info("[DummyService - updatePityStack()] - COMMON 스택 증가 = {}", newCommon);
                return newCommon >= 10;
            }
            else if (wonRarity == RarityType.RARE) {
                Long newRare = redisTemplate.opsForHash().increment(key, "RARE", 1);
                log.info("[DummyService - updatePityStack()] - RARE 스택 증가 = {}", newRare);
                return newRare >= 10;
            }
            else if (wonRarity == RarityType.EPIC) {
                Long newEpic = redisTemplate.opsForHash().increment(key, "EPIC", 1);
                log.info("[DummyService - updatePityStack()] - EPIC 스택 증가 = {}", newEpic);
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

        log.info("[DummyService - getMyDummyList()] - memberId:{} -> dummyIdList.size: {}", memberId, dummyIdList.size());

        return DummyConverter.toGetMyDummyListDTO(memberDummyRepository.findAllByDummyIdList(dummyIdList, PageRequest.of(page, 10)).getContent());
    }

    @Transactional(readOnly = true)
    public List<DummyRespDTO.GetMyDummyDTO> getMyDummyListWithKeyword(Long memberId, String keyword, Integer page){

        Set<Object> members = redisTemplate.opsForSet().members("member:" + memberId + ":dummy");

        if (members.isEmpty()){
            throw new DummyHandler(ErrorCode.DUMMY_NOT_FOUND);
        }

        List<FieldValue> dummyIdList = members.stream()
                .map(id -> FieldValue.of(id.toString()))
                .toList();


        NativeQuery nq = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .multiMatch(mm -> mm
                                                .fields("title.nori^2", "content")
                                                .query(keyword)
                                                .fuzziness("AUTO")
                                                .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                                        )
                                )
                                .should(s -> s
                                        .match(mp -> mp
                                                .field("title")
                                                .query(keyword)
                                                .boost(1.5f)
                                        )
                                )
                                .should(s -> s
                                        .matchPhrase(mp -> mp
                                                .field("title")
                                                .query(keyword)
                                                .boost(3.0f)
                                        )
                                )
                                .should(s -> s
                                        .matchPhrase(mp -> mp
                                                .field("content")
                                                .query(keyword)
                                                .boost(1.0f)
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


    @Transactional
    public Boolean solveQuiz(Long memberId, Long quizId, Integer answer) {

        // 중복 제출 방지
        if (memberQuizRepository.existsByMemberIdAndQuizId(memberId, quizId)) {
            throw new QuizHandler(ErrorCode.ALREADY_SUBMIT);
        }
        /// READ 성공 - 따닥 급의 동일 사용자, n번의 요청

        // 주의! 없을 경우 save() 부분에서 EntityNotFoundException 발생
        Member member = memberRepository.getReferenceById(memberId);

        // Quiz 조회 with PESSIMISTIC_WRITE - ticket 감소 전 배타적 잠금 획득
        ///  MODIFY - n개의 스레드 모두 락 대기 후 획득
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

        ///  WRITE - 락 획득 후 기록 = Concurrency Problem!!!

        // memberGrade은 deprecated. 실제 등수는 아래 Redis list 순서로 집계 예정
        memberQuizRepository.save(MemberQuiz.generateMemberQuiz(member, quiz, 1, answer));

        // 인덱스 == (등수 - 1), CLOSE 시 QuizTask.settleQuizReward()가 상위 5명에게 7일 구독권 지급
        redisTemplate.opsForList().rightPush("quiz:" + quizId, memberId + ":" + answer);

        return true;
    }

}
