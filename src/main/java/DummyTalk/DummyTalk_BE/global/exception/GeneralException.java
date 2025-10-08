package DummyTalk.DummyTalk_BE.global.exception;

import DummyTalk.DummyTalk_BE.global.apiResponse.baseCode.BaseErrorCode;
import DummyTalk.DummyTalk_BE.global.apiResponse.dto.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {
    private BaseErrorCode errorCode;

    public ErrorReasonDTO getReason (){
        return this.errorCode.getReason();
    }

    public ErrorReasonDTO getReasonHttpStatus (){
        return this.errorCode.getReasonHttpStatus();
    }
}
