package DummyTalk.DummyTalk_BE.domain.entity;

import DummyTalk.DummyTalk_BE.domain.entity.common.CommonEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
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

    //    @JsonBackReference

    private Integer reqCount;

    private Boolean isSubscribe;

    private String preReqCnt;

    private LocalDateTime subsExprDate;

    public void updateReqCount(){
        this.reqCount++;
    }

    public void resetReqCount(){
        this.reqCount = 0;
    }

    public void updateSubsExprDate(Boolean isSubscribe, LocalDateTime subsExprDate){
        this.isSubscribe = isSubscribe;
        this.subsExprDate = subsExprDate;
    }
}
