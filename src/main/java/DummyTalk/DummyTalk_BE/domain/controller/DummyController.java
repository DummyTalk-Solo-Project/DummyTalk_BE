package DummyTalk.DummyTalk_BE.domain.controller;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRequestDTO;
import DummyTalk.DummyTalk_BE.domain.entity.user.User;
import DummyTalk.DummyTalk_BE.global.security.userDetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/dummies")
@RequiredArgsConstructor
@Tag(name = "더미 API", description = "일반적인 잡지식을 보게 되는 단방향 대화 API 입니다")
public class DummyController {

    @GetMapping ("/get-dummy")
    public ResponseEntity<Object> dummyTalk (@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody DummyRequestDTO.RequestInfoDTO requestInfoDTO) {

        User user = userDetails.getUser();
        
        // 일반 요청인 경우
        
        // 개발자, 고급 요청인 경우
        
        // 악의적 접근인 경우

        return ResponseEntity.ok("AI 메세지 삽입 예정");
    }
    
    
}
