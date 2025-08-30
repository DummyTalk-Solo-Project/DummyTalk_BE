package DummyTalk.DummyTalk_BE.domain.entity.user;

import DummyTalk.DummyTalk_BE.domain.entity.constant.Login;
import DummyTalk.DummyTalk_BE.domain.entity.info.Info;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    private String password;

    private String username;

    private Login login;

    @OneToOne (mappedBy = "user", cascade = CascadeType.ALL)
    private Info info;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
