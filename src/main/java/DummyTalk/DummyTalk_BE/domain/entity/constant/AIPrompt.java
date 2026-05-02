package DummyTalk.DummyTalk_BE.domain.entity.constant;

import DummyTalk.DummyTalk_BE.domain.entity.Dummy;

public class AIPrompt {
    public static final String GET_QUIZ_PROMPT = "헷갈릴 수 있는 약간 어려운 잡학상식 문제를 낼 것, 다음 요청사항을 정확히 따를 것  ->  " +
            "1. 답변은 JSON 형식에 맞춰 답변할 것 {\"title\": \"니가 내는 문제\", \"answerList\": [\"정답1\", \"정답2\", \"정답3\", \"정답4\"], \"answer\": 정답번호, \"description\": \"정답에 대한 설명\"}, " +
            "2. 답변 말투는 ~요?를 사용하여 친근하면서도 차갑지 않은 중립적의 말투를 사용할 것, " +
            "3. 답변 속의 문제와 4개의 보기는 title과 answerList, answer에 정확히 담을 것, 또한 ```json 등의 코드 블록 문자도 제거할 것, " +
            "4. 최소 70 글자 이상의 문제를 낼 것. 이 규칙을 제일 중요하게 여길 것. " +
            "5. 정답에 대한 설명을 간단하게 적고 description 에 담을 것, " +
            "이후 JSON은 Java class {String title, List<String> answerList, Integer answer} 로 파싱할 예정이니 형식을 엄수할 것";

    public static String generateNewQuizPrompt(Dummy dummy){
        return "현 요청은 ‘AI와 함께하는 무작위 잡학 지식을 얻을 수 있는 Spring 사이드 프로젝트’ DummyTalk 에 저장된 Dummy를 퀴즈로 변환하는 요청이다.\n" +
                "\n" +
                "title: "+dummy.getTitle()+", content: "+dummy.getContent()+"\n" +
                "\n" +
                "다음 제약을 완벽하게 따르고, 정확한 답변을 낼 것.\n" +
                "1. 답변은 JSON 형식에 맞춰 답변할 것 {\"title\": \"니가 내는 문제\", \"answerList\": [\"정답1\", \"정답2\", \"정답3\", \"정답4\"], \"answer\": 정답번호, \"description\": \"정답에 대한 설명\"}, \n" +
                "2. 답변 말투는 ~요?를 사용하여 친근하면서도 차갑지 않은 중립적의 말투를 사용할 것,\n" +
                "3. 답변 속의 문제와 4개의 보기는 title과 answerList, answer에 정확히 담을 것, 또한 ```json 등의 코드 블록 문자도 제거할 것, \n" +
                "4. title과 content를 참고하여, 이를 사용자가 헷갈리기 쉬운 퀴즈로 문제를 변환하여 'title' 에 담을 것. 또한 이에 정답 1개와 헷갈리기 쉬운 오답 3개를 섞어 4지선다 문제를 만들 것.\n" +
                "5. 변환한 문제에 대한 정답은 'answer' (1~4, 0번은 금지) 에담고, 정답에 대한 설명을 간단하게 적고 'description' 에 담을 것\n" +
                "6. 이후 JSON은 Java class {String title, List<String> answerList, Integer answer} 로 파싱할 예정이니 형식을 엄수할 것\"";
    }

}
