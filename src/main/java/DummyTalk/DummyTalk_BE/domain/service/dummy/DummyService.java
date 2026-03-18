package DummyTalk.DummyTalk_BE.domain.service.dummy;

import DummyTalk.DummyTalk_BE.domain.converter.UserConverter;
import DummyTalk.DummyTalk_BE.domain.dto.ChatCompletionResponseDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyResponseDTO;
import DummyTalk.DummyTalk_BE.domain.dto.quiz.QuizResponseDTO;
import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.Member;
import DummyTalk.DummyTalk_BE.domain.entity.Quiz;
import DummyTalk.DummyTalk_BE.domain.entity.Rarity;
import DummyTalk.DummyTalk_BE.domain.entity.constant.AIPrompt;
import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberDummy;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberQuiz;
import DummyTalk.DummyTalk_BE.domain.repository.*;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.handler.DummyHandler;
import DummyTalk.DummyTalk_BE.global.exception.handler.UserHandler;
import DummyTalk.DummyTalk_BE.global.lock.DistributedLock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
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
    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final QuizRepository quizRepository;
    private final MemberQuizRepository memberQuizRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MemberDummyRepository memberDummyRepository;

    @Value("${spring.ai.openai.api-key}")
    private String openAiKey;


    @Transactional
    public DummyResponseDTO.GetDummyRespDTO getDummy(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("Member not found"));

        // 랜덤 조회 알고리즘
        List<Rarity> rarityList = rarityRepository.findAll();
        double pivot = Math.random() * 100;
        double cumulative = 0;

        Rarity selectedRarity = rarityList.getFirst();

        for (Rarity r : rarityList) {
            cumulative += r.getProbability();
            if (pivot <= cumulative) {
                selectedRarity = r;
                break;
            }
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

        return DummyResponseDTO.GetDummyRespDTO.builder()
                .dummyId(dummy.getId())
                .content(dummy.getContent())
                .rarityName(dummy.getRarity().getName())
                .build();
    }

    @Transactional
    public String GetDummyDateForNormal(String email, DummyRequestDTO.RequestInfoDTO requestInfoDTO) {
        return null;
    }


    /**
     * 퀴즈를 만든 후 Redis 저장 및 캐시화
     *
     * @param email
     * @param openQuizDate
     */
    public void openQuiz(String email, LocalDateTime openQuizDate) {
    }


    public DummyResponseDTO.GetQuizInfoResponseDTO getQuiz(String email) {
        return null;
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
