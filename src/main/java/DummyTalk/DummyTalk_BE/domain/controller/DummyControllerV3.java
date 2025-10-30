package DummyTalk.DummyTalk_BE.domain.controller;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.service.dummy.DummyService;
import DummyTalk.DummyTalk_BE.global.security.userDetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

//@RestController
@Slf4j
@RequestMapping("/api/dummies")
@RequiredArgsConstructor
@Tag(name = "더미 API", description = "일반적인 잡지식을 보게 되는 단방향 대화 API 입니다")
public class DummyControllerV3 {
    
    // Security 인증 제거



    private final DummyService dummyService;

    @GetMapping ("/get-dummy")
    public ResponseEntity<Object> dummyTalk (@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody DummyRequestDTO.RequestInfoDTO requestInfoDTO) {

        // 판단하는 거 따로 짜자.

        String aiText = dummyService.GetDummyDateForNormal(userDetails.getUser(), null);
        return ResponseEntity.ok(aiText);
    }

    @PostMapping("/open-quiz")
    public ResponseEntity<?> openQuiz (@AuthenticationPrincipal CustomUserDetails userDetails,
                                       @RequestParam (value = "open-time") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  LocalDateTime date) {

        dummyService.openQuiz(userDetails.getUser(), date);

        return ResponseEntity.ok("open Quiz Success!");
    }

    @GetMapping("/quiz")
    public ResponseEntity<?> getQuiz (@AuthenticationPrincipal CustomUserDetails userDetails){
        return ResponseEntity.ok(dummyService.getQuiz(userDetails.getUser()));
    }

    @PostMapping("/quiz")
    public ResponseEntity<?> solveQuiz (@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("id") Long quizId, @RequestParam("answer") Integer answer){
        dummyService.solveQuiz(userDetails.getUser(), quizId, answer);
        return ResponseEntity.ok("성공적으로 처리 완료!");
    }
}
