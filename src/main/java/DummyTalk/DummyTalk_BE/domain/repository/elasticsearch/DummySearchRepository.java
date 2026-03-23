package DummyTalk.DummyTalk_BE.domain.repository.elasticsearch;

import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DummySearchRepository extends ElasticsearchRepository<Dummy, Long> {
}
