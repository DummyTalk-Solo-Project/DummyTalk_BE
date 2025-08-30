package DummyTalk.DummyTalk_BE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DummyTalkBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(DummyTalkBeApplication.class, args);
	}

}
