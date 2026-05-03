package DummyTalk.DummyTalk_BE.global.exception.handler;

import DummyTalk.DummyTalk_BE.global.apiResponse.baseCode.BaseErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.GeneralException;

public class MemberHandler extends GeneralException {
    public MemberHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
