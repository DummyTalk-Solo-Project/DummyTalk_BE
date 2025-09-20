package DummyTalk.DummyTalk_BE.domain.entity;


import DummyTalk.DummyTalk_BE.domain.entity.common.CommonEntity;
import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.User_Quiz;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quiz extends CommonEntity {

    @Id
    private Long id;

    private QuizStatus status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @OneToMany(mappedBy = "quiz")
    @JsonManagedReference
    private ArrayList<User_Quiz> userQuizList;
}
