package DummyTalk.DummyTalk_BE.domain.controller.dummy;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.service.dummy.impl.DummyServiceImplV3;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@Slf4j
@RequestMapping("/api/dummies")
@RequiredArgsConstructor
@Tag(name = "더미 API", description = "일반적인 잡지식을 보게 되는 단방향 대화 API 입니다")
public class DummyControllerV3 {
    
    // Security 인증 제거

    private final DummyServiceImplV3 dummyService;

    @GetMapping ("/get-dummy")
    public ResponseEntity<Object> dummyTalk (@RequestParam("email") String email, @RequestBody DummyRequestDTO.RequestInfoDTO requestInfoDTO) {

        // 판단하는 거 따로 짜자.

        String aiText = dummyService.GetDummyDateForNormal(email, null);
        return ResponseEntity.ok(aiText);
    }

    @PostMapping("/open-quiz")
    public ResponseEntity<?> openQuiz (@RequestParam("email") String email,
                                       @RequestParam (value = "open-time") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  LocalDateTime date) {

        dummyService.openQuiz(email, date);

        return ResponseEntity.ok("open Quiz Success!");
    }

    @GetMapping("/quiz")
    public ResponseEntity<?> getQuiz (@RequestParam("email") String email){
        return ResponseEntity.ok(dummyService.getQuiz(email));
    }

    @PostMapping("/quiz")
    public ResponseEntity<?> solveQuiz (@RequestParam("email") String email, @RequestParam("answer") Integer answer){
//        dummyService.solveQuiz(userDetails.getUser(), quizId, answer);
        dummyService.solveQuiz(email, 1L, answer);
        return ResponseEntity.ok("성공적으로 처리 완료!");
    }

    // synchronized 사용
    @PostMapping("/quiz2")
    public ResponseEntity<?> solveQuizVer2 (@RequestParam("email") String email, @RequestParam("answer") Integer answer){
        dummyService.solveQuizVer2(email, answer);
        return ResponseEntity.ok("성공적으로 처리 완료!");
    }
    // DistributedLock 사용
    @PostMapping("/quiz3")
    public ResponseEntity<?> solveQuizVer3 (@RequestParam("email") String email, @RequestParam("answer") Integer answer){
        dummyService.solveQuizVer3(email, answer);
        return ResponseEntity.ok("성공적으로 처리 완료!");
    }
}
