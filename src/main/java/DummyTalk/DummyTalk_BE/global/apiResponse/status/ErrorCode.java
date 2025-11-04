package DummyTalk.DummyTalk_BE.global.apiResponse.status;

import DummyTalk.DummyTalk_BE.global.apiResponse.baseCode.BaseErrorCode;
import DummyTalk.DummyTalk_BE.global.apiResponse.dto.ErrorReasonDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements BaseErrorCode {

    // SERVER
    INTERNAL_SERVER_ERROR (HttpStatus.INTERNAL_SERVER_ERROR, "SERVER5000", "서버 에러 입니다,. 에러 코드: SERVER5001, 관리자에게 연락 주시기 바랍니다"),
    PARSING_ERROR (HttpStatus.INTERNAL_SERVER_ERROR, "SERVER5001", "서버 에러 입니다. 에러 코드: SERVER5001, 관리자에게 연락 주시기 바랍니다"),
    AI_PARSING_ERROR (HttpStatus.INTERNAL_SERVER_ERROR, "SERVER5002", "서버 에러 입니다. 에러 코드: SERVER5002, 관리자에게 연락 주시기 바랍니다"),
    CANT_MAKE_EMAIL(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER5003", "서버 에러 입니다. 에러 코드: SERVER5003, 관리자에게 문의해주세요."),
    CANT_SEND_EMAIL(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER5004", "서버 에러 입니다. 에러 코드: SERVER5004, 관리자에게 문의해주세요."),


    // Dummy
    USED_ALL_CHANCES(HttpStatus.BAD_REQUEST, "DUMMY4001", "모든 무료 요청 횟수를 소모하셨네요, 다음을 기약해주세요 :)"),
    NO_SOLVED_QUIZ(HttpStatus.BAD_REQUEST, "DUMMY4002", "풀었던 문제가 없네요. 다음 퀴즈가 열릴 때 까지 같이 기다려봐요"),
    QUIZ_NOT_OPEN(HttpStatus.BAD_REQUEST, "DUMMY4003", "문제를 풀고 싶은 마음은 알겠지만, 조금만 더 기달려주세요."),
    WRONG_QUIZ(HttpStatus.BAD_REQUEST, "DUMMY4004", "알 수 없는 퀴즈를 풀고 계신 것 같아요."),
    WRONG_ANSWER(HttpStatus.BAD_REQUEST, "DUMMY4005", "좋은 발상이었는데, 아쉽게도 그런 정답은 없어요."),
    ALREADY_SUBMIT(HttpStatus.BAD_REQUEST, "DUMMY4006", "한 번 푸셨던 문제는 다시 풀 수 없어요. 다른 사용자에게 배려해주세요 :) "),


    // User
    AUTHORIZATION_REQUIRED(HttpStatus.UNAUTHORIZED, "USER4000", "권한이 부족합니다."),
    WRONG_EMAIL_CODE(HttpStatus.BAD_REQUEST, "USER4001", "제가 보낸 이메일이랑 다른 거 같은데, 다시 한 번 확인해보시겠어요?"),
    EMAIL_EXPIRED(HttpStatus.BAD_REQUEST, "USER4002", "이메일이 너무 오래된 거 같은데, 다시 한 번 보내드릴까요?"),
    CANT_FIND_USER(HttpStatus.BAD_REQUEST, "USER4003", "뭔가 정보가 안맞는데, 다시 한 번 요청해주시겠어요?"),
    ALREADY_REGISTERED (HttpStatus.BAD_REQUEST, "USER4004", "이미 가입한 계정이 있어요");



    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .isSuccess(false)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .isSuccess(false)
                .httpStatus(httpStatus)
                .code(code)
                .message(message)
                .build();
    }
}
