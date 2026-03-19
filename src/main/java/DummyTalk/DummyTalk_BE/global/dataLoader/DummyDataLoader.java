package DummyTalk.DummyTalk_BE.global.dataLoader;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyDataLoadDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.Rarity;
import DummyTalk.DummyTalk_BE.domain.entity.constant.RarityType;
import DummyTalk.DummyTalk_BE.domain.repository.DummyRepository;
import DummyTalk.DummyTalk_BE.domain.repository.RarityRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DummyDataLoader implements ApplicationRunner {

    private final DummyRepository dummyRepository;
    private final RarityRepository rarityRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
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
    }
}
