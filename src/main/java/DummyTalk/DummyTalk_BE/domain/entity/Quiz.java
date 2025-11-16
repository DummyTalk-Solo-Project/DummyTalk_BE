package DummyTalk.DummyTalk_BE.domain.entity;


import DummyTalk.DummyTalk_BE.domain.entity.common.CommonEntity;
import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.UserQuiz;
import DummyTalk.DummyTalk_BE.global.converter.StringListConverter;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quiz extends CommonEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; // 문제

    @Convert(converter =  StringListConverter.class)
    private List<String> answerList = new ArrayList<>(); // 답안

    private Integer answer; // 정답

    private String description; // 답에 대한 설명
    
    private Integer ticket; // 구독권 제한

    private QuizStatus status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @OneToMany(mappedBy = "quiz")
    @JsonManagedReference
    private ArrayList<UserQuiz> userQuizList;
}
