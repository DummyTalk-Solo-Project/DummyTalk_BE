package DummyTalk.DummyTalk_BE.domain.entity.user;

import DummyTalk.DummyTalk_BE.domain.entity.CommonEntity;
import DummyTalk.DummyTalk_BE.domain.entity.constant.Login;
import DummyTalk.DummyTalk_BE.domain.entity.info.Info;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class User extends CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    private String password;

    private String username;

    private Login login;

    @OneToOne (mappedBy = "user", cascade = CascadeType.ALL)
    private Info info;
}