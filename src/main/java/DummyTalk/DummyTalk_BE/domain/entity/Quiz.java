package DummyTalk.DummyTalk_BE.domain.entity;


import DummyTalk.DummyTalk_BE.domain.entity.common.CommonEntity;
import DummyTalk.DummyTalk_BE.domain.entity.constant.QuizStatus;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberQuiz;
import DummyTalk.DummyTalk_BE.global.converter.StringListConverter;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
    private List<MemberQuiz> memberQuizList; // Hibernate 가 프록시 클래스를 사용할 수 없게 해서 변경

    public static Quiz createNewQuiz (String title,
                                      List<String> answerList,
                                      Integer answer,
                                      String description,
                                      Integer ticket,
                                      LocalDateTime startTime){
        return Quiz.builder()
                .title(title)
                .answerList(answerList)
                .answer(answer)
                .description(description)
                .ticket(ticket)
                .status(QuizStatus.NOT_OPEN)
                .startTime(startTime)
                .endTime(startTime.plusMinutes(5))
                .build();
    }

    public Boolean decreaseTicket(){
        if (this.ticket > 0){
            this.ticket--;
            return true;
        }
        return false;
    }
}
