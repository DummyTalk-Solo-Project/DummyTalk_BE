package DummyTalk.DummyTalk_BE.domain.entity;

import DummyTalk.DummyTalk_BE.domain.entity.common.CommonEntity;
import DummyTalk.DummyTalk_BE.domain.entity.constant.Login;
import DummyTalk.DummyTalk_BE.domain.entity.constant.MemberRole;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberBadge;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberDummy;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberQuiz;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Member extends CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    private String password;

    private String memberName;

    @Enumerated(EnumType.STRING)
    private Login login;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    private LocalDateTime lastLogin;

    @Setter
    @OneToOne (mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Info info;

    @OneToMany (mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<MemberDummy> memberDummyList = new ArrayList<>();

    @OneToMany (mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<MemberQuiz> memberQuizList = new ArrayList<>();

    @OneToMany (mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<MemberBadge> memberBadgeList = new ArrayList<>();

    /// deprecated
    @Override
    public String toString() {
        return "{" +
                "email='" + email + '\'' +
                ", password='" + (password != null ? "****" : null) + '\'' +
                ", username='" + memberName + '\'' +
                ", login=" + login +
                '}';
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public void withdraw() { // 회원 탈퇴 처리 - CommonEntity.softDelete()
        softDelete();
    }

    public void restore() { // 계정 복구 - CommonEntity.restore()
        super.restore();
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}