package DummyTalk.DummyTalk_BE.global.apiResponse.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessCode  {
    OK (HttpStatus.OK, "COMMON2000", "성공입니다"),


    // User
    LOGIN_SUCCESS(HttpStatus.OK, "USER2001", "로그인에 성공했습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "USER2002", "로그아웃에 성공했습니다."),
    SIGN_IN_SUCCESS(HttpStatus.CREATED, "USER2003", "회원가입에 성공했습니다."),
    EMAIL_SEND_SUCCESS(HttpStatus.OK, "USER2004", "인증 이메일 발송에 성공했습니다."),
    VALIDATE_SUCCESS (HttpStatus.CONTINUE, "USER2005", "이메일 검증에 성공했습니다"),
    WITHDRAWN_SUCCESS(HttpStatus.OK, "USER2006", "회원 탈퇴에 성공했씁니다."),
    GET_INFO_SUCCESS(HttpStatus.OK, "USER2007", "사용자 정보 조회에 성공했습니다."),
    SUBSCRIBE_SUCCESS(HttpStatus.OK, "USER2007", "구독 요청에 성공했습니다."),

    // Dummy
    GET_DUMMY_SUCCESS(HttpStatus.OK, "DUMMY2000", "더미 요청에 성공하셨습니다."),
    OPEN_QUIZ_SUCCESS(HttpStatus.OK, "DUMMY2001", "(Only Admin) 퀴즈 오픈에 성공하셨습니다."),
    GET_QUIZ_SUCCESS(HttpStatus.OK, "DUMMY2002", "퀴즈 조회에 성공하셨습니다."),
    SOLVE_QUIZ_SUCCESS(HttpStatus.OK, "DUMMY2003", "퀴즈 풀이에 성공하셨습니다."),

    // Test 처리 성공 응답
    TEST_EXCEPTION_SUCCESS(HttpStatus.PARTIAL_CONTENT, "TEST200", " 테스트에 성공했습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;


}

