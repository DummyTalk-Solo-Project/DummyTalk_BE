package DummyTalk.DummyTalk_BE.domain.entity.info;

import DummyTalk.DummyTalk_BE.domain.entity.CommonEntity;
import DummyTalk.DummyTalk_BE.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Info  extends CommonEntity {
    @Id
    @GeneratedValue (strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}
