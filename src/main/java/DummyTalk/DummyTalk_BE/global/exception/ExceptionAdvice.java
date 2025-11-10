package DummyTalk.DummyTalk_BE.global.exception;

import DummyTalk.DummyTalk_BE.global.apiResponse.APIResponse;
import DummyTalk.DummyTalk_BE.global.apiResponse.dto.ErrorReasonDTO;
import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.discord.DiscordNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice(annotations = RestController.class)
public class ExceptionAdvice extends ResponseEntityExceptionHandler {


    @Value("${discord.webhook.url}")
    private String discordWebhookUrl;

    private final DiscordNotificationService discordNotificationService;


    @ExceptionHandler
    public ResponseEntity<Object> exception(Exception ex, WebRequest request) {
        ex.printStackTrace(); // 서버 로그에는 스택 트레이스 전체 출력

        try {
            // WebRequest에서 HttpServletRequest를 추출하여 요청 URL 가져오기
            String requestUri = ((ServletWebRequest)request).getRequest().getRequestURI();

            // 알림 서비스 호출
            discordNotificationService.sendErrorNotification(ex, requestUri);

        } catch (Exception notificationEx) { // 알림 전송 실패가 원래의 예외 처리를 방해해서는 안 됨
            log.error("Failed to send Discord notification: {}", notificationEx.getMessage());
        }

        // 기존 500 에러 응답 반환
        return handleExceptionInternalFalse(ex, ErrorCode.INTERNAL_SERVER_ERROR, HttpHeaders.EMPTY, ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus(), request, ex.getMessage());
    }

    @ExceptionHandler(value = GeneralException.class)
    public ResponseEntity<Object> onThrowExcpetion(GeneralException generalEx, HttpServletRequest request) {
        ErrorReasonDTO reasonHttpStatus = generalEx.getReasonHttpStatus();
        log.error("Exception occurred! : {}", reasonHttpStatus.getMessage());

        if (reasonHttpStatus.getHttpStatus().is5xxServerError()) {
            try {
                discordNotificationService.sendErrorNotification(generalEx, request.getRequestURI());
            } catch (Exception notificationEx) {
                log.error("Failed to send Discord notification for GeneralException: {}", notificationEx.getMessage());
            }
        }

        return handleExceptionInternal(generalEx, reasonHttpStatus, null, request);
    }


    @ExceptionHandler
    public ResponseEntity<Object> validation(ConstraintViolationException ex, WebRequest request) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(constraintViolation -> constraintViolation.getMessage())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Constraint violation exception 추출 도중 에러 발생"));

        return handleExceptionInternalConstraint(ex, ErrorCode.valueOf(errorMessage), HttpHeaders.EMPTY, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().stream()
                .forEach(fieldError -> {
                    String field = fieldError.getField();
                    String errorMessage = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
                    errors.merge(field, errorMessage, (existingErrorMessage, newErrorMessage) -> existingErrorMessage + ", " + newErrorMessage);
                });

        return handleExceptionInternalArgs(ex, HttpHeaders.EMPTY, ErrorCode.valueOf("_BAD_REQUEST"), request, errors);
    }

    /*@ExceptionHandler
    public ResponseEntity<Object    > exception(Exception ex, WebRequest request) {
        ex.printStackTrace();
        return handleExceptionInternalFalse(ex, ErrorCode.INTERNAL_SERVER_ERROR, HttpHeaders.EMPTY, ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus(), request, ex.getMessage());
    }

    @ExceptionHandler(value = GeneralException.class)
    public ResponseEntity<Object> onThrowExcpetion(GeneralException generalEx, HttpServletRequest request) {
        ErrorReasonDTO reasonHttpStatus = generalEx.getReasonHttpStatus();

        log.error("Exception occurred! : {}", reasonHttpStatus.getMessage());

        return handleExceptionInternal(generalEx, reasonHttpStatus, null, request);
    }*/

    private ResponseEntity<Object> handleExceptionInternal(Exception ex, ErrorReasonDTO reason, HttpHeaders headers, HttpServletRequest request) {
        APIResponse<Object> body = APIResponse.onFailure(reason.getCode(), reason.getMessage(), null);
        WebRequest webRequest = new ServletWebRequest(request);
        return super.handleExceptionInternal(ex, body, headers, reason.getHttpStatus(), webRequest);
    }

    private ResponseEntity<Object> handleExceptionInternalConstraint(Exception ex, ErrorCode errorStatus, HttpHeaders headers, WebRequest request) {
        APIResponse<Object> body = APIResponse.onFailure(errorStatus.getCode(), errorStatus.getMessage(), null);
        return super.handleExceptionInternal(ex, body, headers, errorStatus.getHttpStatus(), request);
    }

    private ResponseEntity<Object> handleExceptionInternalArgs(Exception ex, HttpHeaders headers, ErrorCode errorCommonStatus, WebRequest request, Map<String, String> errorArgs) {
        APIResponse<Object> body = APIResponse.onFailure(errorCommonStatus.getCode(), errorCommonStatus.getMessage(), errorArgs);
        return super.handleExceptionInternal(ex, body, headers, errorCommonStatus.getHttpStatus(), request);
    }

    private ResponseEntity<Object> handleExceptionInternalFalse(Exception ex, ErrorCode errorCommonStatus, HttpHeaders headers, HttpStatus status, WebRequest request, String errorPoint) {
        APIResponse<Object> body = APIResponse.onFailure(errorCommonStatus.getCode(), errorCommonStatus.getMessage(), errorPoint);
        return super.handleExceptionInternal(ex, body, headers, status, request
        );
    }
}