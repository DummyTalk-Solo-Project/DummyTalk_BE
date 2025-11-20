package DummyTalk.DummyTalk_BE.domain.controller.dummy;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyResponseDTO;
import DummyTalk.DummyTalk_BE.domain.service.dummy.DummyService;
import DummyTalk.DummyTalk_BE.global.apiResponse.APIResponse;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.SuccessCode;
import DummyTalk.DummyTalk_BE.global.security.userDetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@Slf4j
@RequestMapping("/api/dummies")
@RequiredArgsConstructor
@Tag(name = "더미 API", description = "일반적인 잡지식을 보게 되는 단방향 대화 API 입니다")
public class DummyControllerV2 {

    private final DummyService dummyService;

    @GetMapping ("/get-dummy")
    public APIResponse<String> dummyTalk (@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody DummyRequestDTO.RequestInfoDTO requestInfoDTO) {
        String result = dummyService.GetDummyDateForNormal(userDetails.getUser(), null);
        return APIResponse.onSuccess(result, SuccessCode.GET_DUMMY_SUCCESS);
    }

    @PostMapping("/open-quiz")
    public APIResponse<Object> openQuiz (@AuthenticationPrincipal CustomUserDetails userDetails,
                                         @RequestParam (value = "open-time") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  LocalDateTime date) {

        dummyService.openQuiz(userDetails.getUser(), date);

        return APIResponse.onSuccess(null, SuccessCode.OPEN_QUIZ_SUCCESS);
    }

    @GetMapping("/quiz")
    public APIResponse<DummyResponseDTO.GetQuizInfoResponseDTO> getQuiz (@AuthenticationPrincipal CustomUserDetails userDetails){
        DummyResponseDTO.GetQuizInfoResponseDTO quiz = dummyService.getQuiz(userDetails.getUser());
        return APIResponse.onSuccess(quiz, SuccessCode.GET_QUIZ_SUCCESS);
    }

    @PostMapping("/quiz")
    public APIResponse<Object> solveQuiz (@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("id") Long quizId, @RequestParam("answer") Integer answer){
        dummyService.solveQuiz(userDetails.getUser(), quizId, answer);
        return APIResponse.onSuccess(null, SuccessCode.SOLVE_QUIZ_SUCCESS);
    }
}
