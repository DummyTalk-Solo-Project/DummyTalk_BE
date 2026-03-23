package DummyTalk.DummyTalk_BE.global.dataLoader;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyDataLoadDTO;
import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.Rarity;
import DummyTalk.DummyTalk_BE.domain.entity.constant.RarityType;
import DummyTalk.DummyTalk_BE.domain.entity.document.DummyDocument;
import DummyTalk.DummyTalk_BE.domain.repository.elasticsearch.DummySearchRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.DummyRepository;
import DummyTalk.DummyTalk_BE.domain.repository.jpa.RarityRepository;
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
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final DummySearchRepository dummySearchRepository;

    /*@Override
    public void run(ApplicationArguments args) throws Exception {
        // 1. JSON 파일 읽기
        ClassPathResource resource = new ClassPathResource("data/dummyList.json");
        List<DummyDataLoadDTO> dtoList = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<List<DummyDataLoadDTO>>() {}
        );

        for (DummyDataLoadDTO dto : dtoList) {
            if (!dummyRepository.existsByTitle(dto.getTitle())) {
                RarityType rarityType = RarityType.valueOf(dto.getRarityName().toUpperCase());
                Rarity rarity = rarityRepository.findByName(rarityType)
                        .orElseThrow(() -> new RuntimeException("Rarity not found: " + dto.getRarityName()));


                // PostgreSQL + getId
                Dummy dummy = dummyRepository.save(
                        Dummy.builder()
                                .title(dto.getTitle())
                                .content(dto.getContent())
                                .memberDummyList(new ArrayList<>())
                                .rarity(rarity).build());

                // Redis
                String redisKey = "dummy:" + rarity.getName();
                redisTemplate.opsForSet().add(redisKey, dummy.getId().toString());
            }
        }
    }*/

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        // JSON
        ClassPathResource resource = new ClassPathResource("data/dummyList.json");
        List<DummyDataLoadDTO> dtoList = objectMapper.readValue(
                resource.getInputStream(), new TypeReference<List<DummyDataLoadDTO>>() {});

        Set<String> existingTitles = new HashSet<>(dummyRepository.findAllTitles());

        List<Dummy> newDummies = dtoList.stream()
                .filter(dto -> !existingTitles.contains(dto.getTitle())) // 없는 거만
                .map(dto -> {
                    RarityType type = RarityType.valueOf(dto.getRarityName().toUpperCase());
                    Rarity rarity = rarityRepository.findByName(type).orElseThrow();
                    return Dummy.createDummy(dto, rarity);
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
    }

    private void syncRedisWithDb(List<Dummy> savedDummyList) {
        Map<RarityType, List<String>> collectedDummy = savedDummyList.stream()
                .collect(Collectors.groupingBy(d ->
                        d.getRarity().getName(), Collectors.mapping(d ->
                        d.getId().toString(), Collectors.toList())));

        collectedDummy.forEach((rarityType, id) ->{
            redisTemplate.opsForSet().add("dummy:" + rarityType, id);
        });
    }

}
