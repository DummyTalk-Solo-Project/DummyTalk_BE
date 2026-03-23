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
public class DummyDocument extends CommonEntity {
    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "suggest_analyzer", searchAnalyzer = "standard")
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
