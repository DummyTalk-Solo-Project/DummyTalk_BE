package DummyTalk.DummyTalk_BE.domain.controller;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRespDTO;
import DummyTalk.DummyTalk_BE.domain.service.dummy.DummyService;
import DummyTalk.DummyTalk_BE.global.apiResponse.APIResponse;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.SuccessCode;
import DummyTalk.DummyTalk_BE.global.security.userDetails.CustomUserDetails;
//import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/dummies")
@RequiredArgsConstructor
//@Tag(name = "더미 API", description = "일반적인 잡지식을 보게 되는 단방향 대화 API 입니다")
public class DummyController {

    private final DummyService dummyService;

    @GetMapping ("/dummy")
    public APIResponse<DummyRespDTO.GetDummyRespDTO> getDummy(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return APIResponse.onSuccess(dummyService.getDummy(userDetails.getMember().getId()), SuccessCode.GET_DUMMY_SUCCESS);
    }

    @GetMapping("/my-dummy")
    public APIResponse<List<DummyRespDTO.GetMyDummyDTO>> getMyDummyList (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam (name = "page", defaultValue = "0") Integer page) {
        return APIResponse.onSuccess(dummyService.getMyDummyList(userDetails.getMember().getId(), page), SuccessCode.GET_DUMMY_SUCCESS);
    }

    @GetMapping("/my-dummy/keyword")
    public APIResponse<List<DummyRespDTO.GetMyDummyDTO>> getMyDummyListWithKeyword (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("keyword") String keyword,
            @RequestParam(name="page", defaultValue = "0") Integer page) {
        return APIResponse.onSuccess(dummyService.getMyDummyListWithKeyword(userDetails.getMember().getId(), keyword, page),  SuccessCode.GET_DUMMY_SUCCESS);
    }


    @GetMapping("/quiz")
    public APIResponse<DummyRespDTO.GetQuizInfoResponseDTO> getQuiz (@AuthenticationPrincipal CustomUserDetails userDetails){
        return APIResponse.onSuccess(dummyService.getQuiz(userDetails.getMember().getId()), SuccessCode.GET_QUIZ_SUCCESS);
    }

    @PostMapping("/quiz")
    public APIResponse<Boolean> solveQuiz (@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("id") Long quizId, @RequestParam("answer") Integer answer){
//        dummyServiceInterface.solveQuiz(userDetails.getMember(), quizId, answer);
        return APIResponse.onSuccess(   dummyService.solveQuiz(userDetails.getMember().getId(), quizId, answer), SuccessCode.SOLVE_QUIZ_SUCCESS);
    }

    @GetMapping("/check-quiz")
    public APIResponse<Object> checkQuiz (@AuthenticationPrincipal CustomUserDetails userDetails){
        return APIResponse.onSuccess(dummyService.checkQuiz(userDetails.getMember().getId()), SuccessCode.CHECK_QUIZ_SUCCESS);
    }
}
