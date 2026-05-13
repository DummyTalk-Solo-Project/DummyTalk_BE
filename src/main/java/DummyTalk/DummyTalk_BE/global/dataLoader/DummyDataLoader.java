package DummyTalk.DummyTalk_BE.global.dataLoader;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyDataLoadDTO;
import DummyTalk.DummyTalk_BE.domain.entity.Badge;
import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.Rarity;
import DummyTalk.DummyTalk_BE.domain.entity.constant.RarityType;
import DummyTalk.DummyTalk_BE.domain.entity.document.DummyDocument;
import DummyTalk.DummyTalk_BE.domain.repository.elasticsearch.DummySearchRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.BadgeRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.DummyRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.RarityRepository;
import DummyTalk.DummyTalk_BE.domain.service.badge.BadgeService;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.GeneralException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DummyDataLoader implements ApplicationRunner {

    private final DummyRepository dummyRepository;
    private final RarityRepository rarityRepository;
    private final BadgeRepository badgeRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final DummySearchRepository dummySearchRepository;


    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        Rarity common, rare, epic, special;

        // 초기화되는 겸 처음 키면서 같이 저장
        if (rarityRepository.count() != 4){
            common = rarityRepository.save(Rarity.createRarity(RarityType.COMMON, "F4F0E4", 50.0));
            rare = rarityRepository.save(Rarity.createRarity(RarityType.RARE, "44A194", 30.0));
            epic = rarityRepository.save(Rarity.createRarity(RarityType.EPIC, "537D96", 12.0));
            special = rarityRepository.save(Rarity.createRarity(RarityType.SPECIAL, "EC8F8D", 3.0));
        }
        else{
            common = rarityRepository.findByName(RarityType.COMMON).orElseThrow();
            rare = rarityRepository.findByName(RarityType.RARE).orElseThrow();
            epic = rarityRepository.findByName(RarityType.EPIC).orElseThrow();
            special = rarityRepository.findByName(RarityType.SPECIAL).orElseThrow();
        }

        // JSON
        ClassPathResource resource = new ClassPathResource("data/dummyList.json");
        List<DummyDataLoadDTO> dtoList = objectMapper.readValue(
                resource.getInputStream(), new TypeReference<List<DummyDataLoadDTO>>() {});

        Set<String> existingTitles = new HashSet<>(dummyRepository.findAllTitles());

        List<Dummy> newDummies = dtoList.stream()
                .filter(dto -> !existingTitles.contains(dto.getTitle())) // 없는 거만
                .map(dto -> {
                    if (dto.getRarityName().toUpperCase().equals("COMMON")) {
                        return Dummy.createDummy(dto, common);
                    }
                    else if (dto.getRarityName().toUpperCase().equals("RARE")) {
                        return Dummy.createDummy(dto, rare);
                    }
                    else if (dto.getRarityName().toUpperCase().equals("EPIC")) {
                        return Dummy.createDummy(dto, epic);
                    }
                    else if (dto.getRarityName().toUpperCase().equals("SPECIAL")) {
                        return Dummy.createDummy(dto, special);
                    }
                    else{
                        throw new GeneralException(ErrorCode.WRONG_RARITY);
                    }
                })
                .toList();

        if (!newDummies.isEmpty()) {
            List<Dummy> savedDummyList = dummyRepository.saveAll(newDummies);

            List<DummyDocument> dummyDocumentList = savedDummyList.stream()
                    .map(DummyDocument::createDummyDocument)
                    .toList();

            dummySearchRepository.saveAll(dummyDocumentList);

            syncRedisWithDb(savedDummyList); // Redis 동기화
        }

        initBadges(); // 뱃지 초기화 작업
    }

    // 뱃지 초기 데이터 적재 (없는 뱃지만 추가)
    private void initBadges() {
        List<Badge> initialBadges = List.of(
                Badge.builder()
                        .name(BadgeService.BADGE_FIRST_STEP)
                        .content("첫 번째 더미를 조회하셨어요! 더미톡에 오신 걸 환영합니다!").build(),
                Badge.builder()
                        .name(BadgeService.BADGE_REGULAR)
                        .content("더미를 10번이나 조회하셨군요! 벌써 단골이세요?").build(),
                Badge.builder()
                        .name(BadgeService.BADGE_ENTHUSIAST)
                        .content("무려 100번 더미를 조회하셨어요! 스몰토크 정도의 잡상식은 늘었겠죠?").build(),
                Badge.builder()
                        .name(BadgeService.BADGE_PITY)
                        .content("천장이 발동됐어요! 운명이 당신 편이었군요.").build(),
                Badge.builder()
                        .name(BadgeService.BADGE_LEGEND)
                        .content("SPECIAL 등급 획득! 이 확률을 뚫으셨다니, 전설의 시작이에요.").build()
        );

        for (Badge badge : initialBadges) {
            if (!badgeRepository.existsByName(badge.getName())) {
                badgeRepository.save(badge);
            }
        }
    }

    private void syncRedisWithDb(List<Dummy> savedDummyList) {

        savedDummyList.forEach(d ->
                redisTemplate.opsForSet().add("dummy:" + d.getRarity().getName(), d.getId())
                );
    }
}