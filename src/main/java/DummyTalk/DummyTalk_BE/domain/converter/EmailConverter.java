package DummyTalk.DummyTalk_BE.domain.converter;

import DummyTalk.DummyTalk_BE.domain.entity.email.Email;

import java.time.LocalDateTime;

public class EmailConverter {
    public static Email toNewEmail(String email, String code, LocalDateTime expireTime) {
        return Email.builder()
                .email(email)
                .code(code)
                .expireTime(expireTime)
                .build();
    }
}
