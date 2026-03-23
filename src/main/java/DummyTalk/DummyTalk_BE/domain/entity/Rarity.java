package DummyTalk.DummyTalk_BE.domain.entity;

import DummyTalk.DummyTalk_BE.domain.entity.constant.RarityType;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Rarity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    private RarityType name;

    @Column(nullable = false)
    private String colorCode; // F4F0E4, 44A194, 537D96, EC8F8D

    @Column(nullable = false)
    private Double probability; // 55% 30% 12% 3%

    @OneToMany (mappedBy = "rarity", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Dummy> dummyList = new ArrayList<>();

    public static Rarity defaultRarity (){
        Rarity rarity = new Rarity();
        rarity.name=RarityType.COMMON;
        return rarity;
    }

    public static Rarity createRarity (RarityType name, String colorCode, Double probability){
        return Rarity.builder()
                .name(name)
                .colorCode(colorCode)
                .probability(probability)
                .build();
    }
}
