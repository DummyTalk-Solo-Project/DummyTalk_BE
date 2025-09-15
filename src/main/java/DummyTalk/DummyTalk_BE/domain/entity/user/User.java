package DummyTalk.DummyTalk_BE.domain.entity.user;

import DummyTalk.DummyTalk_BE.domain.entity.CommonEntity;
import DummyTalk.DummyTalk_BE.domain.entity.constant.Login;
import DummyTalk.DummyTalk_BE.domain.entity.dummy.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.info.Info;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    @Setter
    @OneToOne (mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Info info;

    @OneToMany (mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Dummy> dummyList = new ArrayList<Dummy>();

    @Override
    public String toString() {
        return "{" +
                "email='" + email + '\'' +
                ", password='" + (password != null ? "****" : null) + '\'' +
                ", username='" + username + '\'' +
                ", login=" + login +
                '}';
    }

}