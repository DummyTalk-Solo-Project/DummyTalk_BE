package DummyTalk.DummyTalk_BE.global.apiResponse.status;

import DummyTalk.DummyTalk_BE.global.apiResponse.baseCode.BaseErrorCode;
import DummyTalk.DummyTalk_BE.global.apiResponse.dto.ErrorReasonDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements BaseErrorCode {

    // CLIENT
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "CLIENT4000", "로그인이 필요해요"),
    // 동일 사용자의 중복(따닥) 요청 — IdempotentRequestInterceptor에서 발생
    DUPLICATE_REQUEST(HttpStatus.TOO_MANY_REQUESTS, "CLIENT4291", "이미 처리 중인 요청이 있어요. 잠시 후 다시 시도해주세요."),


    // SERVER
    INTERNAL_SERVER_ERROR (HttpStatus.INTERNAL_SERVER_ERROR, "SERVER5000", "서버 에러 입니다,. 에러 코드: SERVER5001, 관리자에게 연락 주시기 바랍니다"),
    PARSING_ERROR (HttpStatus.INTERNAL_SERVER_ERROR, "SERVER5001", "서버 에러 입니다. 에러 코드: SERVER5001, 관리자에게 연락 주시기 바랍니다"),
    AI_PARSING_ERROR (HttpStatus.INTERNAL_SERVER_ERROR, "SERVER5002", "서버 에러 입니다. 에러 코드: SERVER5002, 관리자에게 연락 주시기 바랍니다"),
    CANT_MAKE_EMAIL(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER5003", "서버 에러 입니다. 에러 코드: SERVER5003, 관리자에게 문의해주세요."),
    CANT_SEND_EMAIL(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER5004", "서버 에러 입니다. 에러 코드: SERVER5004, 관리자에게 문의해주세요."),
    CANT_CONVERT_TO_DB_COLUMN (HttpStatus.INTERNAL_SERVER_ERROR, "SERVER5005", "서버 에러 입니다. 에러 코드: SERVER5005, 관리자에게 문의해주세요."),
    CANT_CONVERT_TO_ENTITY_ATTR (HttpStatus.INTERNAL_SERVER_ERROR, "SERVER5006", "서버 에러 입니다. 에러 코드: SERVER5006, 관리자에게 문의해주세요."),
    CANT_ENCODE_STRING (HttpStatus.INTERNAL_SERVER_ERROR, "SERVER5007", "서버 에러 입니다. 에러 코드: SERVER5007, 관리자에게 문의해주세요."),
    CANT_GET_LOCK (HttpStatus.TOO_MANY_REQUESTS, "SERVER5008", "이미 처리 중인 요청이 있어요. 잠시 후 다시 시도해주세요."),


    // SECURITY
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "SERVER_4100", "인증 정보가 없습니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "SERVER_4101", "토큰이 만료되었습니다."),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "SERVER_4102", "유효하지 않은 토큰입니다."),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "SERVER_4103", "블랙리스트에 있는 토큰입니다."),
    CANNOT_FOUND_RT(HttpStatus.UNAUTHORIZED, "SERVER_4104", "리프레쉬 토큰을 찾을 수 없습니다."),
    RT_NOT_FOUND(HttpStatus.UNAUTHORIZED, "G008", "리프레쉬 토큰을 찾을 수 없습니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "SERVER_4300", "접근 권한이 없습니다."),
    CANT_FIND_AUTHORITIES(HttpStatus.FORBIDDEN, "SERVER_4300", "권한 정보가 존재하지 않습니다."),


    // Dummy
    WRONG_DUMMY(HttpStatus.INTERNAL_SERVER_ERROR, "DUMMY5001", "알 수 없는 더미 입니다."),
    DUMMY_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "DUMMY5002", "더미를 찾을 수 없습니다."),
    DUMMY_WITH_RARITY_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "DUMMY5003", "해당 등급의 더미를 찾을 수 없습니다."),
    DUMMY_WITH_ID_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "DUMMY5003", "해당 아이디의 더미를 찾을 수 없습니다."),

    USED_ALL_CHANCES(HttpStatus.BAD_REQUEST, "DUMMY4001", "모든 무료 요청 횟수를 소모하셨네요, 다음을 기약해주세요 :)"),


    // Rarity
    WRONG_RARITY(HttpStatus.INTERNAL_SERVER_ERROR, "RARITY5001", "등급을 찾을 수 없습니다"),


    // QUIZ
    NO_SOLVED_QUIZ(HttpStatus.BAD_REQUEST, "QUIZ4001", "풀었던 문제가 없네요. 다음 퀴즈가 열릴 때 까지 같이 기다려봐요"),
    QUIZ_NOT_OPEN(HttpStatus.BAD_REQUEST, "QUIZ4002", "문제를 풀고 싶은 마음은 알겠지만, 조금만 더 기달려주세요."),
    WRONG_QUIZ(HttpStatus.BAD_REQUEST, "QUIZ4003", "알 수 없는 퀴즈를 풀고 계신 것 같아요."),
    WRONG_ANSWER(HttpStatus.BAD_REQUEST, "QUIZ4004", "좋은 발상이었는데, 아쉽게도 정답이 아니에요."),
    ALREADY_SUBMIT(HttpStatus.BAD_REQUEST, "QUIZ4005", "한 번 푸셨던 문제는 다시 풀 수 없어요. 다른 사용자에게 배려해주세요 :) "),
    TICKET_IS_DONE (HttpStatus.BAD_REQUEST, "QUIZ4006", "퀴즈는 풀었지만 이제 티켓을 받을 수는 없네요"),
    QUIZ_IS_CLOSED (HttpStatus.BAD_REQUEST, "QUIZ4007", "아쉽게도 퀴즈가 닫혔어요...."),
    QUIZ_INVALID_OPEN_TIME(HttpStatus.BAD_REQUEST, "QUIZ4008", "퀴즈 오픈 시간은 현재 시간 이후여야 해요."),


    // Member
    AUTHORIZATION_REQUIRED(HttpStatus.UNAUTHORIZED, "USER4000", "권한이 부족합니다."),
    WRONG_EMAIL_CODE(HttpStatus.BAD_REQUEST, "MEMBER4001", "제가 보낸 이메일이랑 다른 거 같은데, 다시 한 번 확인해보시겠어요?"),
    EMAIL_EXPIRED(HttpStatus.BAD_REQUEST, "MEMBER4002", "이메일이 너무 오래된 거 같은데, 다시 한 번 보내드릴까요?"),
    CANT_FIND_MEMBER(HttpStatus.BAD_REQUEST, "MEMBER4003", "뭔가 정보가 안맞는데, 다시 한 번 요청해주시겠어요?"),
    ALREADY_REGISTERED (HttpStatus.BAD_REQUEST, "MEMBER4004", "이미 가입한 계정이 있어요"),
    ALREADY_SEND(HttpStatus.BAD_REQUEST, "MEMBER4005", "이미 이메일을 보냈어요, 다시 한 번 확인해주시겠어요?."),
    EXIST_MEMBER(HttpStatus.BAD_REQUEST, "MEMBER4006", "해당 이메일은 이미 누가 쓰고 있어요."),
    MEMBER_NOT_FOUND(HttpStatus.BAD_REQUEST, "MEMBER4007", "누군 지 모르겠어요!"),
    MEMBER_WITHDRAWN(HttpStatus.UNAUTHORIZED, "MEMBER4008", "이미 탈퇴한 계정입니다."),
    // 탈퇴 후 2주 이내 — 프론트에서 복구 다이얼로그 표시 트리거
    MEMBER_WITHDRAWN_RESTORABLE(HttpStatus.FORBIDDEN, "MEMBER4009", "탈퇴한 계정이에요. 2주 이내라면 계정을 되살릴 수 있어요!"),
    // 탈퇴 후 2주 초과 — 영구 탈퇴 상태
    MEMBER_WITHDRAWN_EXPIRED(HttpStatus.GONE, "MEMBER4010", "탈퇴 후 2주가 지나 복구가 불가능해요."),
    WRONG_PASSWORD(HttpStatus.BAD_REQUEST, "MEMBER4011", "현재 비밀번호가 일치하지 않아요."),
    ALREADY_SUBSCRIBED(HttpStatus.BAD_REQUEST, "MEMBER4012", "이미 구독 중이에요!"), // 근데 늘리는 방식으로 가지 않을까


    // ADMIN
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN4001", "해당 날짜의 정산 데이터가 없습니다."),


    // NOTICE
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTICE4001", "공지사항을 찾을 수 없습니다."),
    // isPublished=false인 공지사항을 일반 사용자가 조회 시
    NOTICE_NOT_PUBLISHED(HttpStatus.NOT_FOUND, "NOTICE4002", "공개되지 않은 공지사항입니다.");




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
