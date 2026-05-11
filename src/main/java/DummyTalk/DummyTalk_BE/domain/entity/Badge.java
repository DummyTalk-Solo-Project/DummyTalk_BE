package DummyTalk.DummyTalk_BE.domain.entity;

import DummyTalk.DummyTalk_BE.domain.entity.common.CommonEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Badge extends CommonEntity { // CommonEntity -> 뱃지 취득일 확인 가능
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // 실질적인 뱃지 이름. ex '열정적인 더미톡유저'

    @Column(unique = true, nullable = false)
    private String content; // 뱃지 취득 이유. ex "더미를 100번 조회하셨어요! 그래도 스몰토크 정도의 잡상식은 늘어났을 거에요!"

    @Column
    private String imageUrl; // 뱃지 이미지 경로. /uploads/badges/{filename} 형태 - (EC2 /home/ubuntu/data/badges/ 마운트)
}
