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
                        throw new RuntimeException("Unknown rarity");
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