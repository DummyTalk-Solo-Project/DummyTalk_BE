package DummyTalk.DummyTalk_BE;

import jakarta.persistence.EntityManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories(
		basePackages = "DummyTalk.DummyTalk_BE.domain.repository.jpa"
)
@EnableElasticsearchRepositories(
		basePackages = "DummyTalk.DummyTalk_BE.domain.repository.elasticsearch"
)
@EnableScheduling
public class DummyTalkBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(DummyTalkBeApplication.class, args);
	}


}
