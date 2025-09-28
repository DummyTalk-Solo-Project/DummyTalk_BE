package DummyTalk.DummyTalk_BE.global.exception.handler;

import DummyTalk.DummyTalk_BE.global.apiResponse.baseCode.BaseErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.GeneralException;

public class DummyHandler extends GeneralException {
    public DummyHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
