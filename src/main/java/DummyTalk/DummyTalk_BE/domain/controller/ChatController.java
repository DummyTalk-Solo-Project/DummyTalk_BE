package DummyTalk.DummyTalk_BE.domain.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/chats")
@Tag(name = "채팅 API", description = "일반적인 잡지식을 보게 되는 단방향 대화 API 입니다")
public class ChatController {

    @GetMapping ("/dummy")
    public ResponseEntity<Object> dummyTalk (){

        return ResponseEntity.ok(null);
    }
}
