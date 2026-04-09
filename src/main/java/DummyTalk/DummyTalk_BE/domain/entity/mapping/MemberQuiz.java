package DummyTalk.DummyTalk_BE.domain.entity.mapping;


import DummyTalk.DummyTalk_BE.domain.entity.Quiz;
import DummyTalk.DummyTalk_BE.domain.entity.Member;
import DummyTalk.DummyTalk_BE.domain.entity.common.CommonEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberQuiz extends CommonEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /// deprecated
    private Integer memberGrade;

    private Integer answer;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    public static MemberQuiz generateMemberQuiz(Member member, Quiz quiz, Integer grade, Integer answer) {
        return MemberQuiz.builder()
                .memberGrade(grade)
                .answer(answer)
                .member(member)
                .quiz(quiz)
                .build();
    }
}
