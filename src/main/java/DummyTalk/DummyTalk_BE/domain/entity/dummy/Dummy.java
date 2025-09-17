package DummyTalk.DummyTalk_BE.domain.entity.dummy;

import DummyTalk.DummyTalk_BE.domain.entity.CommonEntity;
import DummyTalk.DummyTalk_BE.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dummy extends CommonEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean isUserContent;

    @Column(columnDefinition = "TEXT")
    private String request;

    private String response;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
