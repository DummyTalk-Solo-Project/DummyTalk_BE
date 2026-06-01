package DummyTalk.DummyTalk_BE.domain.entity.document;

import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
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

    // mainField(suggest_analyzer): 색인 시 Nori 형태소 + edge_ngram(1~10) → [강, 강아, 강아지] 부분 일치용
    // title.nori(InnerField): edge_ngram 없이 nori 형태소만 색인 → fuzziness와 충돌 없이 오타 교정 전용
    // 두 역할을 분리해야 하는 이유: edge_ngram 색인 위에 fuzzy를 적용하면 단어 길이 불일치로 매칭이 불안정

    private Long rarityId;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "suggest_analyzer", searchAnalyzer = "nori"),
            otherFields = {
                    @InnerField(suffix = "nori", type = FieldType.Text, analyzer = "nori", searchAnalyzer = "nori")
            }
    )
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
