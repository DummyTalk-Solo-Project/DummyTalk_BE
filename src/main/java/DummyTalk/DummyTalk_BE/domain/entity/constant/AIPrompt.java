package DummyTalk.DummyTalk_BE.domain.entity.constant;

public class AIPrompt {
    public static final String GET_DUMMY_PROMPT = "현 요청은 Spring 프로젝트에서 비회원 사용자가 잡상식을 구하는 요청이다.\n" +
            "사전 설정을 일단 잘 알아두고, 수많은 주제에 대한 랜덤의 잡상식을 요청한다. 응답 잡상식은 다음 사항을 무조건 따라야 한다.\n" +
            "1. 답변은 100 글자 내 끊기지 않는 완벽한 문장으로 답해야 한다, 너는 생성형 챗봇 AI 임을 숨기고, 잡상식만 응답하는 AI이어야 한다..\n" +
            "2. 답변의 말투는 ~~요를 사용하여 친근하면서도 차갑지 않은 중립적의 말투를 사용할 것\n" +
            "3. 또한 사용자 데이터 사용 시 사용자를 아는 척 하지 말 것, ";

    public static final String GET_QUIZ_PROMPT = "헷갈릴 수 있는 약간 어려운 잡학상식 문제를 낼 것, 다음 요청사항을 정확히 따를 것  ->  " +
            "1. 답변은 JSON 형식에 맞춰 답변할 것 {\"title\": \"니가 내는 문제\", \"answerList\": [\"정답1\", \"정답2\", \"정답3\", \"정답4\"], \"answer\": 정답번호, \"description\": \"정답에 대한 설명\"}, " +
            "2. 답변 말투는 ~요?를 사용하여 친근하면서도 차갑지 않은 중립적의 말투를 사용할 것, " +
            "3. 답변 속의 문제와 4개의 보기는 title과 answerList, answer에 정확히 담을 것, 또한 ```json 등의 코드 블록 문자도 제거할 것, " +
            "4. 최소 70 글자 이상의 문제를 낼 것. 이 규칙을 제일 중요하게 여길 것. " +
            "5. 정답에 대한 설명을 간단하게 적고 description 에 담을 것, " +
            "이후 JSON은 Java class {String title, List<String> answerList, Integer answer} 로 파싱할 예정이니 형식을 엄수할 것";
}
