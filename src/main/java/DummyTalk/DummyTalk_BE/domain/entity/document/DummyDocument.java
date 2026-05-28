package DummyTalk.DummyTalk_BE.domain.entity.document;

import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.common.CommonEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Document(indexName = "dummy")
@Setting(settingPath = "elasticsearch/settings.json")
public class DummyDocument {
    @Id
    private Long id;

    // analyzer=suggest_analyzer: 색인 시 Nori 형태소 + edge_ngram(1~10)으로 [강, 강아, 강아지] 형태로 저장 → 자동완성·부분일치 지원
    // searchAnalyzer=nori: 검색 시 [강아지] 토큰이 edge_ngram 토큰과 매칭되도록 한글 형태소 기반 검색
    // standard를 쓰면 공백 기준 분리만 하므로 nori가 생성한 한글 형태소 토큰과 매칭 신뢰도 하락

    private Long rarityId;

    @Field(type = FieldType.Text, analyzer = "suggest_analyzer", searchAnalyzer = "nori")
    private String title;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String content;

    @Field(type = FieldType.Keyword)
    private String rarityName;

    public static DummyDocument createDummyDocument(Dummy dummy) {
        return DummyDocument.builder()
                .id(dummy.getId())
                .title(dummy.getTitle())
                .content(dummy.getContent())
                .rarityName(dummy.getRarity().getName().toString())
                .build();
    }
}
