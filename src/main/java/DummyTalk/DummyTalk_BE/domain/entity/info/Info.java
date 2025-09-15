package DummyTalk.DummyTalk_BE.domain.entity.info;

import DummyTalk.DummyTalk_BE.domain.entity.CommonEntity;
import DummyTalk.DummyTalk_BE.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    private Integer reqCount;

    private Boolean isSubscribe;

    private String preReqCnt;

    private LocalDateTime subsExprDate;

    public void updateReqCount(){
        this.reqCount++;
    }
}
