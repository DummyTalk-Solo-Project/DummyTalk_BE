package DummyTalk.DummyTalk_BE.domain.entity;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyDataLoadDTO;
import DummyTalk.DummyTalk_BE.domain.entity.common.CommonEntity;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberDummy;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;


import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Dummy extends CommonEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content; // 막 길지는 않아서 충분할 듯? 이것만 쓰지 않을까?

    @OneToMany (mappedBy = "dummy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<MemberDummy> memberDummyList = new ArrayList<>();

    @ManyToOne (fetch = FetchType.EAGER)
    @JoinColumn(name = "rarity_id")
    private Rarity rarity;

    public static Dummy createDummy(DummyDataLoadDTO dto, Rarity rarity) {
        return Dummy.builder()
                .title(dto.getTitle())
                .content(dto.getTitle())
                .rarity(rarity).build();
    }
}
