package DummyTalk.DummyTalk_BE.domain.entity.mapping;

import DummyTalk.DummyTalk_BE.domain.entity.common.CommonEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class MemberBadge extends CommonEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 안에 담을 내용이 딱히 없는 듯, 획득일 = 수정일 이라.
}
