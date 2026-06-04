package DummyTalk.DummyTalk_BE.domain.controller;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRespDTO;
import DummyTalk.DummyTalk_BE.domain.service.dummy.DummyService;
import DummyTalk.DummyTalk_BE.global.apiResponse.APIResponse;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.SuccessCode;
import DummyTalk.DummyTalk_BE.global.interceptor.annotation.IdempotentRequest;
import DummyTalk.DummyTalk_BE.global.security.userDetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/dummies")
@RequiredArgsConstructor
public class DummyController {

    private final DummyService dummyService;

    /**
     * K6 동시성 전략 비교 버전 전환 설정 (application.yml -> concurrency.getDummy-version)
     * 각 번호 순서대로 테스트 하기
     */
    @Value("${concurrency.getDummy-version:3}")
    private int getDummyVersion;

    @IdempotentRequest
    @GetMapping ("/dummy")
    public APIResponse<DummyRespDTO.GetDummyRespDTO> getDummy(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMember().getId();
        return APIResponse.onSuccess(
            switch (getDummyVersion) {
                case 1  -> dummyService.getDummyV1(memberId); // 순수 트랜잭션
                case 2  -> dummyService.getDummyV2(memberId); // 분산락
                default -> dummyService.getDummy(memberId);   // 인터셉터 (최신 버전쓰)
            },
            SuccessCode.GET_DUMMY_SUCCESS
        );
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

    @IdempotentRequest
    @PostMapping("/quiz")
    public APIResponse<Boolean> solveQuiz (@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("id") Long quizId, @RequestParam("answer") Integer answer){
        return APIResponse.onSuccess(   dummyService.solveQuiz(userDetails.getMember().getId(), quizId, answer), SuccessCode.SOLVE_QUIZ_SUCCESS);
    }
}
