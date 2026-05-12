package DummyTalk.DummyTalk_BE.domain.service.email;

import DummyTalk.DummyTalk_BE.global.apiResponse.status.ErrorCode;
import DummyTalk.DummyTalk_BE.global.exception.GeneralException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EMailService {

    private final JavaMailSender mailSender;
    private final RedisTemplate redisTemplate;

    @Async("mailExecutor")
    public void startMailWorker(int workerId) {
        log.info("[EMailService - startMailWorker()] - EmailWorker 메일 발송 워커 가동 시작");
        while (true) {
            try {
                // BRPOP!
                String task = (String) redisTemplate.opsForList().rightPop("email_queue", 5, TimeUnit.SECONDS);

                if (task != null) {
                    String[] data = task.split(":");
                    sendEmail(data[0], data[1]);
                }if (task == null && workerId == 0) {
                    log.info("[EMailService - startMailWorker()] - (worker {}) no email to send!", workerId);
                }
            } catch (Exception e) {
                log.error("[EMailService - startMailWorker()] - EmailWorker {} Error, retry after 1s... ", workerId, e);
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException ignored) { }
            }
        }
    }
    public void sendEmail(String email, String code) {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper;

        try {
            helper = new MimeMessageHelper(msg, true, "utf-8");
            helper.setTo(email);
            helper.setFrom("no-reply@mail.com", "no-reply@mail.com");
            helper.setSubject("Lumo 인증 이메일 알림.");
            helper.setText("<!DOCTYPE html>\n" +
                    "<html lang=\"ko\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "    <style>\n" +
                    "        @import url('https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;700&display=swap');\n" +
                    "        \n" +
                    "        body {\n" +
                    "            font-family: 'Noto Sans KR', 'Malgun Gothic', 'Apple SD Gothic Neo', Arial, sans-serif;\n" +
                    "            background-color: #f4f6f9;\n" +
                    "            margin: 0;\n" +
                    "            padding: 0;\n" +
                    "        }\n" +
                    "        .container {\n" +
                    "            width: 100%;\n" +
                    "            max-width: 580px;\n" +
                    "            margin: 40px auto;\n" +
                    "            background-color: #ffffff;\n" +
                    "            border-radius: 12px;\n" +
                    "            box-shadow: 0 6px 15px rgba(0, 0, 0, 0.05);\n" +
                    "            overflow: hidden;\n" +
                    "            border: 1px solid #e9ecef;\n" +
                    "        }\n" +
                    "        .header {\n" +
                    "            background-color: #ffffff;\n" +
                    "            padding: 30px 20px 20px;\n" +
                    "            text-align: center;\n" +
                    "        }\n" +
                    "        .header h2 {\n" +
                    "            font-size: 24px;\n" +
                    "            color: #212529;\n" +
                    "            font-weight: 700;\n" +
                    "            margin: 0;\n" +
                    "        }\n" +
                    "        .content {\n" +
                    "            padding: 20px;\n" +
                    "            text-align: center;\n" +
                    "            line-height: 1.6;\n" +
                    "            color: #495057;\n" +
                    "        }\n" +
                    "        .verification-code-container {\n" +
                    "            margin: 30px 0;\n" +
                    "            text-align: center;\n" +
                    "        }\n" +
                    "        .verification-code {\n" +
                    "            display: inline-block;\n" +
                    "            background-color: #eaf3ff;\n" +
                    "            padding: 15px 30px;\n" +
                    "            font-size: 28px;\n" +
                    "            font-weight: bold;\n" +
                    "            letter-spacing: 3px;\n" +
                    "            border-radius: 8px;\n" +
                    "            color: #0056b3;\n" +
                    "            border: 1px dashed #ced4da;\n" +
                    "            -webkit-user-select: all;\n" +
                    "            -moz-user-select: all;\n" +
                    "            -ms-user-select: all;\n" +
                    "            user-select: all;\n" +
                    "        }\n" +
                    "        .instruction {\n" +
                    "            margin-top: 10px;\n" +
                    "            font-size: 14px;\n" +
                    "            color: #6c757d;\n" +
                    "        }\n" +
                    "        .footer {\n" +
                    "            text-align: center;\n" +
                    "            padding: 20px;\n" +
                    "            border-top: 1px solid #e9ecef;\n" +
                    "            font-size: 12px;\n" +
                    "            color: #adb5bd;\n" +
                    "            background-color: #f8f9fa;\n" +
                    "        }\n" +
                    "        .logo {\n" +
                    "            width: 50px;\n" +
                    "            height: 50px;\n" +
                    "            background-color: #007bff;\n" +
                    "            border-radius: 50%;\n" +
                    "            margin: 0 auto 15px;\n" +
                    "        }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<div class=\"container\">\n" +
                    "    <div class=\"header\">\n" +
                    "        \n" +
                    "        <div class=\"logo\"></div>\n" +
                    "        <h2>Lumo 이메일 인증</h2>\n" +
                    "    </div>\n" +
                    "    <div class=\"content\">\n" +
                    "        <p>안녕하세요, Lumo입니다. 이메일 주소 인증을 위해 아래 코드를 사용해 주세요.</p>\n" +
                    "        <div class=\"verification-code-container\">\n" +
                    "            <span class=\"verification-code\">  " + code + "  </span>\n" +
                    "        </div>\n" +
                    "        <p>이 코드는 3분 동안 유효합니다.</p>\n" +
                    "        <p class=\"instruction\">코드를 복사하여 앱에 붙여넣어 주세요.</p>\n" +
                    "    </div>\n" +
                    "    <div class=\"footer\">\n" +
                    "        <p>이 메일은 발신 전용입니다. 문의사항은 관리자 이메일로 문의 부탁 드립니다.</p>\n" +
                    "        <p>&copy; 2026 Lumo. All Rights Reserved.</p>\n" +
                    "    </div>\n" +
                    "</div>\n" +
                    "</body>\n" +
                    "</html>", true);
            helper.setReplyTo("no-reply@mail.com");
            mailSender.send(msg);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new GeneralException(ErrorCode.CANT_SEND_EMAIL);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        log.info("[EMailService - sendEmail()] - saved code {} to {}", redisTemplate.opsForValue().get(email), email);
    }


    @Async("mailExecutor")
    public void sendPasswordResetEmailAsync(String email, String tempPassword) {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper;
        try {
            helper = new MimeMessageHelper(msg, true, "utf-8");
            helper.setTo(email);
            helper.setFrom("no-reply@mail.com", "no-reply@mail.com");
            helper.setSubject("더미톡 임시 비밀번호 안내");
            helper.setText("<!DOCTYPE html>\n" +
                    "<html lang=\"ko\"><head><meta charset=\"UTF-8\">" +
                    "<style>body{font-family:'Noto Sans KR',Arial,sans-serif;background-color:#f4f6f9;margin:0;padding:0;}" +
                    ".container{width:100%;max-width:580px;margin:40px auto;background:#fff;border-radius:12px;" +
                    "box-shadow:0 6px 15px rgba(0,0,0,.05);overflow:hidden;border:1px solid #e9ecef;}" +
                    ".header{padding:30px 20px 20px;text-align:center;}" +
                    ".header h2{font-size:24px;color:#212529;font-weight:700;margin:0;}" +
                    ".content{padding:20px;text-align:center;line-height:1.6;color:#495057;}" +
                    ".temp-pw{display:inline-block;background-color:#fff3cd;padding:15px 30px;font-size:28px;" +
                    "font-weight:bold;letter-spacing:3px;border-radius:8px;color:#856404;" +
                    "border:1px dashed #ced4da;-webkit-user-select:all;user-select:all;}" +
                    ".footer{text-align:center;padding:20px;border-top:1px solid #e9ecef;font-size:12px;" +
                    "color:#adb5bd;background-color:#f8f9fa;}</style></head>" +
                    "<body><div class=\"container\">" +
                    "<div class=\"header\"><h2>더미톡 임시 비밀번호</h2></div>" +
                    "<div class=\"content\">" +
                    "<p>안녕하세요, 더미톡입니다. 아래 임시 비밀번호로 로그인 후 반드시 비밀번호를 변경해 주세요.</p>" +
                    "<div><span class=\"temp-pw\">  " + tempPassword + "  </span></div>" +
                    "<p style=\"margin-top:20px;font-size:14px;color:#6c757d;\">본인이 요청하지 않은 경우 즉시 고객센터로 문의하세요.</p>" +
                    "</div>" +
                    "<div class=\"footer\"><p>이 메일은 발신 전용입니다.</p>" +
                    "<p>&copy; 2025 DummyTalk. All Rights Reserved.</p></div>" +
                    "</div></body></html>", true);
            helper.setReplyTo("no-reply@mail.com");
            mailSender.send(msg);
            log.info("[EMailService - sendPasswordResetEmailAsync()] - 임시 비밀번호 발송 완료: {}", email);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("[EMailService - sendPasswordResetEmailAsync()] - 발송 실패: {}", email, e);
            throw new GeneralException(ErrorCode.CANT_SEND_EMAIL);
        }
    }

    public void sendReminderEmail(String toEmail, String nickname) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[DummyTalk] " + nickname + "님, 보고 싶어요!");
        message.setText("안녕하세요 " + nickname + "님!\n\n" +
                "DummyTalk을 떠나신 지 5일이 지났네요.\n" +
                "새로운 대화들이 당신을 기다리고 있습니다. 지금 바로 접속해 보세요!");

        mailSender.send(message);
    }
}