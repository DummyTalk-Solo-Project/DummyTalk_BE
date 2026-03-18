package DummyTalk.DummyTalk_BE.domain.dto.dummy;


import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.Rarity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DummyDataLoadDTO {
    private String title;
    private String content;
    private String rarityName;

    public static Dummy toEntity (DummyDataLoadDTO dto, Rarity rarity) {

    }
}
