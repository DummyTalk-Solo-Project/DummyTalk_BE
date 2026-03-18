package DummyTalk.DummyTalk_BE.domain.entity;

import DummyTalk.DummyTalk_BE.domain.entity.common.CommonEntity;
import DummyTalk.DummyTalk_BE.domain.entity.constant.Login;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberQuiz;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Member extends CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    private String password;

    private String memberName;

    private Login login;

    private LocalDateTime lastLogin;

    @Setter
    @OneToOne (mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Info info;

    // @JsonManagedReference

    @OneToMany (mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Dummy> dummyList = new ArrayList<>();

    @OneToMany (mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<MemberQuiz> memberQuizList = new ArrayList<>();

    @Override
    public String toString() {
        return "{" +
                "email='" + email + '\'' +
                ", password='" + (password != null ? "****" : null) + '\'' +
                ", username='" + username + '\'' +
                ", login=" + login +
                '}';
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
}