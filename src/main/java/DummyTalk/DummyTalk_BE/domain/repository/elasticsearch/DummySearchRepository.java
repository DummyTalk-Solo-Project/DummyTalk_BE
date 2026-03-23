package DummyTalk.DummyTalk_BE.domain.repository.elasticsearch;

import DummyTalk.DummyTalk_BE.domain.entity.document.DummyDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DummySearchRepository extends ElasticsearchRepository<DummyDocument, Long> {
}
