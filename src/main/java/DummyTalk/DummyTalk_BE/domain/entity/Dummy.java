package DummyTalk.DummyTalk_BE.domain.entity;

import DummyTalk.DummyTalk_BE.domain.entity.common.CommonEntity;
import DummyTalk.DummyTalk_BE.domain.entity.mapping.MemberDummy;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

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

    /// deprecated
    private Boolean isUserContent;

    /// deprecated
    @Column(columnDefinition = "TEXT")
    private String request;

    /// deprecated
    private String response;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content; // 막 길지는 않아서 충분할 듯?


    @OneToMany (mappedBy = "dummy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<MemberDummy> memberDummyList = new ArrayList<>();

    @ManyToOne (fetch = FetchType.EAGER)
    @JoinColumn(name = "rarity_id")
    private Rarity rarity;
}
