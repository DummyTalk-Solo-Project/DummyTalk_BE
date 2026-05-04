package DummyTalk.DummyTalk_BE.domain.dto.member;

import DummyTalk.DummyTalk_BE.global.security.jwt.JWT;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class MemberRespDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginSuccessDTO{
        private Boolean isSuccess;
        private String memberName;
        private String accessToken;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfoDTO {
        private JWT jwt;
        private String username;
    }

    // 마이페이지 뱃지 항목
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BadgeDTO {
        private String name;
        private String content;
        private LocalDateTime acquiredAt; // MemberBadge.createdAt
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class GetMemberResponseDTO {
        private String memberName;
        private String email;

        // member - info
        private Integer reqCount;
        private Boolean isSubscribe;
        private LocalDateTime subsExprDate;

        // 등급별 천장 스택 (Redis pity:{memberId})
        private Integer commonStack;
        private Integer rareStack;
        private Integer epicStack;
        
        private List<BadgeDTO> badgeList; // 보유 뱃지
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FindEmailRespDTO{
        private String email;
    }
}
