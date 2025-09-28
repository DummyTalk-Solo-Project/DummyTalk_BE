package DummyTalk.DummyTalk_BE.global.apiResponse.baseCode;

import DummyTalk.DummyTalk_BE.global.apiResponse.dto.ErrorReasonDTO;

public interface BaseErrorCode {

    ErrorReasonDTO getReason();

    ErrorReasonDTO getReasonHttpStatus();

}