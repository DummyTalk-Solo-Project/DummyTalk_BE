package DummyTalk.DummyTalk_BE.domain.entity;

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
    private String name;

    @Column(nullable = false)
    private String colorCode;

    @Column(nullable = false)
    private Double probability;

    @OneToMany (mappedBy = "rarity", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Dummy> dummyList = new ArrayList<>();

    public static Rarity defaultRarity (){

        Rarity rarity = new Rarity();
        rarity.name="COMMON";
        return rarity;
    }
}
