package DummyTalk.DummyTalk_BE.global.discord;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.lettuce.core.pubsub.PubSubOutput.Type.message;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordNotificationService {

    private final RestTemplate restTemplate;

    @Value("${discord.webhook.url}")
    private String discordWebhookUrl;

    public void sendErrorNotification(Exception ex, String requestUri) {

        if (discordWebhookUrl == null || discordWebhookUrl.isBlank()) {
            log.warn("Discord Webhook URL이 설정되지 않아 500 에러 알림을 보낼 수 없습니다.");
            return;
        }

        try {

            String safeMessage = safe(ex.getMessage());
            String safeUri = safe(requestUri);

            String stackTrace = Arrays.stream(ex.getStackTrace())
                    .map(StackTraceElement::toString)
                    .map(this::safe)
                    .collect(Collectors.joining("\\n")); // ← Discord 에서 개행은 \\n 형태가 훨씬 안정적

            if (stackTrace.length() > 900) {
                stackTrace = stackTrace.substring(0, 900) + "...(truncated)";
            }

            DiscordField f1 = new DiscordField("요청 URL", safeUri, false);
            DiscordField f2 = new DiscordField("에러 메시지", safeMessage, false);
            DiscordField f3 = new DiscordField("스택 트레이스", stackTrace, false);

            DiscordEmbed embed = new DiscordEmbed(
                    "500 Internal Server Error 발생",
                    "서버에서 처리되지 않은 예외가 발생했습니다.",
                    15158332,
                    Instant.now().toString(),
                    List.of(f1, f2, f3)
            );

            DiscordPayload payload = new DiscordPayload(null, List.of(embed));

            restTemplate.postForObject(discordWebhookUrl, payload, String.class);

        } catch (Exception e) {
            log.error("Discord 알림 전송 중 예외 발생: {}", e.getMessage());
        }
    }

    private String safe(String text) {
        if (text == null) return "";

        return text // 자꾸 충돌나서 chatGPT 도움
                .replace("`", "")
                .replace("*", "")
                .replace("_", "")
                .replace("|", "")
                .replace(">", "")
                .replace("\t", " ")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    // 밑은 dTO
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class DiscordPayload {
        private String content;
        private List<DiscordEmbed> embeds;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class DiscordEmbed {
        private String title;
        private String description;
        private int color;
        private String timestamp; // ISO8601
        private List<DiscordField> fields;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class DiscordField {
        private String name;
        private String value;
        private boolean inline;
    }
}
