package DummyTalk.DummyTalk_BE.global.apiResponse.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessCode  {
    OK (HttpStatus.OK, "COMMON2000", "성공입니다"),


    // MEMBER
    LOGIN_SUCCESS(HttpStatus.OK, "MEMBER2001", "로그인에 성공했습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "MEMBER2002", "로그아웃에 성공했습니다."),
    SIGN_IN_SUCCESS(HttpStatus.CREATED, "MEMBER2003", "회원가입에 성공했습니다."),
    EMAIL_SEND_SUCCESS(HttpStatus.OK, "MEMBER2004", "인증 이메일 발송에 성공했습니다."),
    VALIDATE_SUCCESS (HttpStatus.OK, "MEMBER2005", "이메일 검증에 성공했습니다"),
    WITHDRAWN_SUCCESS(HttpStatus.OK, "MEMBER2006", "회원 탈퇴에 성공했습니다."),
    GET_INFO_SUCCESS(HttpStatus.OK, "MEMBER2007", "사용자 정보 조회에 성공했습니다."),
    SUBSCRIBE_SUCCESS(HttpStatus.OK, "MEMBER2008", "구독 요청에 성공했습니다."),
    CHECK_EMAIL_SUCCESS(HttpStatus.OK, "MEMBER2009", "사용 가능한 이메일입니다."),
    FIND_EMAIL_SUCCESS(HttpStatus.OK, "MEMBER2010", "이메일 확인에 성공했습니다."),
    PASSWORD_RESET_SUCCESS(HttpStatus.OK, "MEMBER2011", "임시 비밀번호가 이메일로 발송되었습니다."),
    PASSWORD_CHANGE_SUCCESS(HttpStatus.OK, "MEMBER2012", "비밀번호 변경에 성공했습니다."),
    SUBSCRIPTION_POPUP_SUCCESS(HttpStatus.OK, "MEMBER2013", "구독 팝업 확인에 성공했습니다."),

    // Dummy
    GET_DUMMY_SUCCESS(HttpStatus.OK, "DUMMY2000", "더미 요청에 성공하셨습니다."),
    OPEN_QUIZ_SUCCESS(HttpStatus.OK, "DUMMY2001", "(Only Admin) 퀴즈 오픈에 성공하셨습니다."),
    GET_QUIZ_SUCCESS(HttpStatus.OK, "DUMMY2002", "퀴즈 조회에 성공하셨습니다."),
    SOLVE_QUIZ_SUCCESS(HttpStatus.OK, "DUMMY2003", "퀴즈 풀이에 성공하셨습니다."),
    CHECK_QUIZ_SUCCESS(HttpStatus.OK, "DUMMY2004", "퀴즈 체킹에 성공하셨습니다."),

    // ADMIN
    GET_SETTLEMENT_SUCCESS(HttpStatus.OK, "ADMIN2001", "정산 데이터 조회에 성공했습니다."),
    OPEN_QUIZ_ADMIN_SUCCESS(HttpStatus.OK, "ADMIN2002", "(Admin) 퀴즈 오픈에 성공했습니다."),
    APPROVE_SUBSCRIBE_SUCCESS(HttpStatus.OK, "ADMIN2003", "(Admin) 구독 승인에 성공했습니다."),

    // NOTICE
    GET_NOTICE_SUCCESS(HttpStatus.OK, "NOTICE2001", "공지사항 조회에 성공했습니다."),
    CREATE_NOTICE_SUCCESS(HttpStatus.CREATED, "NOTICE2002", "공지사항 작성에 성공했습니다."),
    UPDATE_NOTICE_SUCCESS(HttpStatus.OK, "NOTICE2003", "공지사항 수정에 성공했습니다."),
    DELETE_NOTICE_SUCCESS(HttpStatus.OK, "NOTICE2004", "공지사항 삭제에 성공했습니다."),
    TOGGLE_NOTICE_PUBLISH_SUCCESS(HttpStatus.OK, "NOTICE2005", "공지사항 공개 상태가 변경되었습니다."),

    // Test 처리 성공 응답
    TEST_EXCEPTION_SUCCESS(HttpStatus.PARTIAL_CONTENT, "TEST200", " 테스트에 성공했습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;


}

