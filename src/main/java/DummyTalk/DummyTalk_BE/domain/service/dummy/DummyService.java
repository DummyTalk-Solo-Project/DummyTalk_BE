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
import DummyTalk.DummyTalk_BE.global.lock.DistributedLock;
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
    private final RarityRepository rarityRepository;      // V1에서 DB 기반 확률 조회에 사용
    private final DummyRepository dummyRepository;
    private final MemberDummyRepository memberDummyRepository;
    private final QuizRepository quizRepository;

    private final ElasticsearchOperations elasticsearchOperations;
    private final RedisTemplate<String, Object> redisTemplate;

    private final MemberQuizRepository memberQuizRepository;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * 순수 @Transactional — 동시성 전략 비교 기준선 (베이스라인)
     *
     * 기존 특징
     *  - Rarity를 DB에서 직접 조회 (rarityRepository.findAll → 확률 계산)
     *  - 뱃지용 totalDummyCount를 DB COUNT 쿼리로 조회 (O(N))
     *  - 동시성 보호 없음 → 따닥 요청 시 reqCount 중복 증가 발생
     *  - 총 쿼리 약 7번 발생
     *
     * 비교 목적: K6 stage1 — race_condition_suspect 발생 여부 확인
     */
    @Transactional
    public DummyRespDTO.GetDummyRespDTO getDummyV1(Long memberId) {
        // READ
        Member member = memberRepository.findByIdFetchJoinInfo(memberId).orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));
        Info info = member.getInfo();

        if ((info.getIsSubscribe() && info.getReqCount() >= 40) || (!info.getIsSubscribe() && info.getReqCount() >= 20)) {
            throw new DummyHandler(ErrorCode.USED_ALL_CHANCES);
        }

        String pityKey = "pity:" + memberId;
        Map<Object, Object> pity = redisTemplate.opsForHash().entries(pityKey);

        int currentCommonStack = Integer.parseInt(pity.getOrDefault("COMMON", "0").toString());
        int currentRareStack   = Integer.parseInt(pity.getOrDefault("RARE",   "0").toString());
        int currentEpicStack   = Integer.parseInt(pity.getOrDefault("EPIC",   "0").toString());

        Boolean isPityTriggered = false;
        // DB에서 Rarity 엔티티 직접 로드 (V1 특징 — Redis 없이 DB 조회)
        Rarity selectedRarity = Rarity.defaultRarity();

        if (currentCommonStack >= 10) {
            selectedRarity = rarityRepository.findByName(RarityType.RARE).orElseThrow(() -> new DummyHandler(ErrorCode.WRONG_RARITY));
            isPityTriggered = true;
            log.info("[DummyService - getDummyV1()] - COMMON 천장 사용 -> RARE!");
        } else if (currentRareStack >= 10) {
            selectedRarity = rarityRepository.findByName(RarityType.EPIC).orElseThrow(() -> new DummyHandler(ErrorCode.WRONG_RARITY));
            isPityTriggered = true;
            log.info("[DummyService - getDummyV1()] - RARE 천장 사용 -> EPIC!");
        } else if (currentEpicStack >= 10) {
            selectedRarity = rarityRepository.findByName(RarityType.SPECIAL).orElseThrow(() -> new DummyHandler(ErrorCode.WRONG_RARITY));
            isPityTriggered = true;
            log.info("[DummyService - getDummyV1()] - EPIC 천장 사용 -> SPECIAL!");
        } else {
            // DB에서 확률 조회 후 랜덤 선택 (Redis 미사용 — V1 특징)
            selectedRarity = getRandomRarityFromDB();
            log.info("[DummyService - getDummyV1()] - 랜덤 뽑기: {}", selectedRarity.getName());
        }

        Boolean isNextPityTriggered = updatePityStack(pityKey, selectedRarity.getName(), isPityTriggered);

        Object result = redisTemplate.opsForSet().randomMember("dummy:" + selectedRarity.getName());
        if (result == null) throw new DummyHandler(ErrorCode.WRONG_RARITY);
        Long randomDummyId = Long.valueOf(result.toString());

        Dummy dummy = dummyRepository.findByIdWithRarity(randomDummyId).orElseThrow(() -> new DummyHandler(ErrorCode.WRONG_DUMMY));

        memberDummyRepository.save(MemberDummy.generateMemberDummy(member, dummy));
        redisTemplate.opsForSet().add("member:" + memberId + ":dummy", dummy.getId());

        // V1 특징: DB COUNT 쿼리로 누적 횟수 조회 (데이터 많을수록 O(N))
        long totalDummyCount = memberDummyRepository.countByMember_Id(memberId);

        // WRITE — 동시성 보호 없음 (V1 의도적 미적용)
        info.updateReqCount();

        eventPublisher.publishEvent(new DummyViewedEvent(memberId, dummy.getRarity().getName().toString(), isPityTriggered, totalDummyCount));

        return DummyRespDTO.GetDummyRespDTO.builder()
                .dummyId(dummy.getId())
                .title(dummy.getTitle())
                .content(dummy.getContent())
                .rarityName(dummy.getRarity().getName().toString())
                .isPityTriggered(isPityTriggered)
                .isNextPityTriggered(isNextPityTriggered)
                .remainingCount((Boolean.TRUE.equals(info.getIsSubscribe()) ? 40 : 20) - info.getReqCount())
                .build();
    }

    /**
     * @Transactional + @DistributedLock — 분산락 도입 버전
     *
     * 총정리
     *  - waitTime=0: 락 획득 실패 시 즉시 CANT_GET_LOCK 반환 (따닥 방지)
     *  - Redis 기반 확률 조회 (DB 쿼리 4번으로 감소)
     *  - 뱃지용 totalDummyCount를 Redis O(1) 카운터로 조회
     *  - @DistributedLock AOP가 @Transactional 바깥쪽에서 실행 (@Order(1))
     *    → 락 획득 → 트랜잭션 시작 → 로직 → 트랜잭션 커밋 → 락 해제 순서 보장
     *
     * K6 stage2 — 분산락만으로 DB 커넥션 안정화 측정 및 동시성 해결 테스트
     */
    @Transactional
    @DistributedLock(key = "'lock:getDummy:' + #memberId", waitTime = 0, leaseTime = 4)
    public DummyRespDTO.GetDummyRespDTO getDummyV2(Long memberId) {
        Member member = memberRepository.findByIdFetchJoinInfo(memberId).orElseThrow(() -> new MemberHandler(ErrorCode.MEMBER_NOT_FOUND));
        Info info = member.getInfo();

        if ((info.getIsSubscribe() && info.getReqCount() >= 40) || (!info.getIsSubscribe() && info.getReqCount() >= 20)) {
            throw new DummyHandler(ErrorCode.USED_ALL_CHANCES);
        }

        String pityKey = "pity:" + memberId;
        Map<Object, Object> pity = redisTemplate.opsForHash().entries(pityKey);

        int currentCommonStack = Integer.parseInt(pity.getOrDefault("COMMON", "0").toString());
        int currentRareStack   = Integer.parseInt(pity.getOrDefault("RARE",   "0").toString());
        int currentEpicStack   = Integer.parseInt(pity.getOrDefault("EPIC",   "0").toString());

        Boolean isPityTriggered = false;
        RarityType selectedRarityType;

        if (currentCommonStack >= 10) {
            selectedRarityType = RarityType.RARE;
            isPityTriggered = true;
            log.info("[DummyService - getDummyV2()] - COMMON 천장 사용 -> RARE!");
        } else if (currentRareStack >= 10) {
            selectedRarityType = RarityType.EPIC;
            isPityTriggered = true;
            log.info("[DummyService - getDummyV2()] - RARE 천장 사용 -> EPIC!");
        } else if (currentEpicStack >= 10) {
            selectedRarityType = RarityType.SPECIAL;
            isPityTriggered = true;
            log.info("[DummyService - getDummyV2()] - EPIC 천장 사용 -> SPECIAL!");
        } else {
            selectedRarityType = getRandomRarityType();
            log.info("[DummyService - getDummyV2()] - 랜덤 뽑기: {}", selectedRarityType);
        }

        Boolean isNextPityTriggered = updatePityStack(pityKey, selectedRarityType, isPityTriggered);

        Object result = redisTemplate.opsForSet().randomMember("dummy:" + selectedRarityType);
        if (result == null) throw new DummyHandler(ErrorCode.WRONG_RARITY);
        Long randomDummyId = Long.valueOf(result.toString());

        Dummy dummy = dummyRepository.findByIdWithRarity(randomDummyId).orElseThrow(() -> new DummyHandler(ErrorCode.WRONG_DUMMY));

        memberDummyRepository.save(MemberDummy.generateMemberDummy(member, dummy));
        redisTemplate.opsForSet().add("member:" + memberId + ":dummy", dummy.getId());

        String countKey = "count:" + memberId;
        Object existing = redisTemplate.opsForValue().get(countKey);
        long currentCount;
        if (existing == null) {
            currentCount = memberDummyRepository.countByMember_Id(memberId);
            redisTemplate.opsForValue().set(countKey, currentCount);
        } else {
            currentCount = Long.parseLong(existing.toString());
        }
        redisTemplate.opsForValue().increment(countKey);
        long totalDummyCount = currentCount + 1;

        info.updateReqCount();

        eventPublisher.publishEvent(new DummyViewedEvent(memberId, dummy.getRarity().getName().toString(), isPityTriggered, totalDummyCount));

        return DummyRespDTO.GetDummyRespDTO.builder()
                .dummyId(dummy.getId())
                .title(dummy.getTitle())
                .content(dummy.getContent())
                .rarityName(dummy.getRarity().getName().toString())
                .isPityTriggered(isPityTriggered)
                .isNextPityTriggered(isNextPityTriggered)
                .remainingCount((Boolean.TRUE.equals(info.getIsSubscribe()) ? 40 : 20) - info.getReqCount())
                .build();
    }

    /**
     * @Transactional + IdempotentRequestInterceptor
     *
     * 총정리
     * - @DistributedLock 제거 — 인터셉터가 따닥 방지 담당
     * - 인터셉터에서 막히지 않은 요청만 이 메서드에 도달
     * - BadgeEventListener: @TransactionalEventListener(AFTER_COMMIT) + @Async 로 커밋 후 처리
     *   → 뱃지 체크 시 MemberDummy 영속 완료 보장
     *
     * 이후 Virtual Thread 도입 및 관련 Redission 충돌 사항이 있어서
     * - 이건 천천히
     *
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

        // DB 조회 없이 RarityType 직접 결정 - 확률은 Redis에서 관리 (DummyDataLoader가 적재)
        RarityType selectedRarityType;

        if (currentCommonStack >= 10) { // COMMON -> RARE
            selectedRarityType = RarityType.RARE;
            isPityTriggered = true;
            log.info("[DummyService - getDummy()] - COMMON 천장 사용 -> RARE!");
        }
        else if (currentRareStack >= 10) { // RARE -> EPIC
            selectedRarityType = RarityType.EPIC;
            isPityTriggered = true;
            log.info("[DummyService - getDummy()] - RARE 천장 사용 -> EPIC!");
        }
        else if (currentEpicStack >= 10) { // EPIC -> SPECIAL
            selectedRarityType = RarityType.SPECIAL;
            isPityTriggered = true;
            log.info("[DummyService - getDummy()] - EPIC 천장 사용 -> SPECIAL!");
        }
        else {
            // 2. 천장 없는 경우 Redis 확률 기반 추첨 (rarity:probabilities 해시)
            selectedRarityType = getRandomRarityType();
            log.info("[DummyService - getDummy()] - 랜덤 뽑기: {}", selectedRarityType);
        }

        // 스택 update, 다음 뽑기 천장 예정 여부 반환
        Boolean isNextPityTriggered = updatePityStack(pityKey, selectedRarityType, isPityTriggered);

        // {dummy:등급} set에 저장되어 있는 id 중 하나 랜덤으로 긁어옴
        Object result = redisTemplate.opsForSet().randomMember("dummy:" + selectedRarityType);
        if (result == null) {
            throw new DummyHandler(ErrorCode.WRONG_RARITY);
        }
        Long randomDummyId = Long.valueOf(result.toString());

        // 한 번에 찾기
        Dummy dummy = dummyRepository.findByIdWithRarity(randomDummyId).orElseThrow(() -> new DummyHandler(ErrorCode.WRONG_DUMMY));

        // 조회 기록으로 저장
        memberDummyRepository.save(MemberDummy.generateMemberDummy(member, dummy));
        redisTemplate.opsForSet().add("member:"+memberId+":dummy", dummy.getId());

        // 뱃지 체크용 누적 횟수
        // PostgreSQL -> Redis O(1) 카운터 (enableTransactionSupport로 분리)
        String countKey = "count:" + memberId;
        Object existing = redisTemplate.opsForValue().get(countKey); // READ는 즉시 실행
        long currentCount;
        if (existing == null) {
            // 최초 접근 or Redis 재시작 시에만 DB 조회 후 초기화
            currentCount = memberDummyRepository.countByMember_Id(memberId);
            redisTemplate.opsForValue().set(countKey, currentCount); // WRITE - 큐잉, 나중에
        } else {
            currentCount = Long.parseLong(existing.toString());
        }
        redisTemplate.opsForValue().increment(countKey); // WRITE - 큐잉 (커밋 시 반영)
        long totalDummyCount = currentCount + 1; // 큐잉 전 값 + 1 = 커밋 후 실제 값


        ///  WRITE - Concurrency Problem!!!
        info.updateReqCount();

        // 뱃지 이벤트 발행
        // BadgeEventListener: @TransactionalEventListener(AFTER_COMMIT) + @Async("BadgeExecutor")
        // → 이 publishEvent() 호출은 트랜잭션 커밋 전이지만,
        //   실제 리스너 실행은 AFTER_COMMIT 이후 비동기 처리 → MemberDummy 영속 완료 보장
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
        log.info("[DummyService - getDummy()] - selectedRarity: {}", selectedRarityType);
        return dto;
    }

    // 스택 업데이트 후 다음 뽑기가 천장 확정인지 반환 (increment 반환값 >= 10)
    private Boolean updatePityStack(String key, RarityType wonRarity, Boolean isPityTriggered) {

        // READ → calculate → WRITE(queued) → return expected

        if (isPityTriggered) {
            if (wonRarity == RarityType.SPECIAL) {
                // SPECIAL은 최상위 등급, 다음 천장 없음
                redisTemplate.opsForHash().put(key, "EPIC", "0");
                log.info("[DummyService - updatePityStack()] - EPIC 천장! => SPECIAL");
                return false;
            }
            else if (wonRarity == RarityType.EPIC) {
                Object currentEpic = redisTemplate.opsForHash().get(key, "EPIC");
                long nextEpic = (currentEpic == null ? 0L : Long.parseLong(currentEpic.toString())) + 1;

                redisTemplate.opsForHash().put(key, "RARE", "0");
                redisTemplate.opsForHash().increment(key, "EPIC", 1);

                log.info("[DummyService - updatePityStack()] - RARE 천장! => EPIC, newEpicStack={}", nextEpic);
                return nextEpic >= 10;
            }
            else if (wonRarity == RarityType.RARE) {
                Object currentRare = redisTemplate.opsForHash().get(key, "RARE");
                long nextRare = (currentRare == null ? 0L : Long.parseLong(currentRare.toString())) + 1;

                redisTemplate.opsForHash().put(key, "COMMON", "0");
                redisTemplate.opsForHash().increment(key, "RARE", 1); // 누락됐던 RARE 증가
                log.info("[DummyService - updatePityStack()] - COMMON 천장! => RARE, newRareStack={}", nextRare);
                return nextRare >= 10;
            }
        }
        else {
            if (wonRarity == RarityType.COMMON) {
                Object currentCommon = redisTemplate.opsForHash().get(key, "COMMON");
                long nextCommon = (currentCommon == null ? 0L : Long.parseLong(currentCommon.toString())) + 1;
                redisTemplate.opsForHash().increment(key, "COMMON", 1); // WRITE: 큐잉
                log.info("[DummyService - updatePityStack()] - COMMON 스택 증가 = {}", nextCommon);
                return nextCommon >= 10;
            }
            else if (wonRarity == RarityType.RARE) {
                Object currentRare = redisTemplate.opsForHash().get(key, "RARE");
                long nextRare = (currentRare == null ? 0L : Long.parseLong(currentRare.toString())) + 1;
                redisTemplate.opsForHash().increment(key, "RARE", 1); // WRITE: 큐잉
                log.info("[DummyService - updatePityStack()] - RARE 스택 증가 = {}", nextRare);
                return nextRare >= 10;
            }
            else if (wonRarity == RarityType.EPIC) {
                Object currentEpic = redisTemplate.opsForHash().get(key, "EPIC");
                long nextEpic = (currentEpic == null ? 0L : Long.parseLong(currentEpic.toString())) + 1;
                redisTemplate.opsForHash().increment(key, "EPIC", 1); // WRITE: 큐잉
                log.info("[DummyService - updatePityStack()] - EPIC 스택 증가 = {}", nextEpic);
                return nextEpic >= 10;
            }
        }
        return false;
    }

    /**
     * [V1 전용] DB에서 Rarity 목록을 조회해 확률 기반 랜덤 선택.
     * V2/V3는 Redis 캐시를 사용하는 getRandomRarityType()으로 대체됨.
     * rarityRepository.findAll() → 최대 4개 레코드 (COMMON/RARE/EPIC/SPECIAL)
     */
    private Rarity getRandomRarityFromDB() {
        List<Rarity> rarityList = rarityRepository.findAll();
        double pivot = Math.random() * 100;
        double cumulative = 0;
        for (int i = 0; i < rarityList.size(); i++) {
            Rarity r = rarityList.get(i);
            cumulative += r.getProbability();
            if (pivot <= cumulative || i == rarityList.size() - 1) {
                return r;
            }
        }
        return rarityList.get(0); // fallback
    }

    // Redis rarity:probabilities 해시 기반 확률 선택 (V2/V3)
    public RarityType getRandomRarityType() {
        Map<Object, Object> probs = redisTemplate.opsForHash().entries("rarity:probabilities"); // READ - 즉시 실행
        double pivot = Math.random() * 100;
        double cumulative = 0;
        RarityType[] order = {RarityType.COMMON, RarityType.RARE, RarityType.EPIC, RarityType.SPECIAL};
        for (int i = 0; i < order.length; i++) {
            Object prob = probs.get(order[i].name());
            if (prob == null) continue;
            cumulative += Double.parseDouble(prob.toString());
            if (pivot <= cumulative || i == order.length - 1) {
                return order[i];
            }
        }
        return RarityType.COMMON; // fallback (확률 합 < 100일 때 부동소수점 처리)
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
    @DistributedLock(key = "'lock:solveQuiz:' + #quizId", waitTime = 5, leaseTime = 5)
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
