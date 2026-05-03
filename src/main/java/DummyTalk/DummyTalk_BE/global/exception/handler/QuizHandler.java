package DummyTalk.DummyTalk_BE.global.exception.handler;

import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.GeneralException;

public class QuizHandler extends GeneralException {
    public QuizHandler(ErrorCode errorCode) {
        super(errorCode);
    }
}
