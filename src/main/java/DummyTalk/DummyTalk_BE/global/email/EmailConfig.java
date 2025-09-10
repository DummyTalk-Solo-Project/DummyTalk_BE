package DummyTalk.DummyTalk_BE.global.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class EmailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private Integer port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender (){
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(host);
        javaMailSender.setPort(port);
        javaMailSender.setUsername(username);
        javaMailSender.setPassword(password);

        Properties properties = new Properties();
        properties.put("mail.smtp.starttls.enable", "true"); // STARTTLS 활성화
        properties.put("mail.smtp.auth", "true");
        properties.put ("mail.smtp.connectiontimeout", 5000);
        properties.put ("mail.smtp.timeout", 5000);
        properties.put ("mail.smtp.writetimeout", 5000);

        javaMailSender.setJavaMailProperties(properties);

        return javaMailSender;
    }



}
