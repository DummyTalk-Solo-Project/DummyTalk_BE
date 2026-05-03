package DummyTalk.DummyTalk_BE.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

@Configuration
public class ElasticSearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.data.elasticsearch.url}")
    private String esUrl;

    @Override
    public ClientConfiguration clientConfiguration() {
        // connectedTo()는 "host:port" 형식만 허용하므로 http(s):// 스킴 제거
        String hostAndPort = esUrl.replaceFirst("^https?://", "");
        return ClientConfiguration.builder()
                .connectedTo(hostAndPort)
                .build();
    }

}
