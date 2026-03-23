package DummyTalk.DummyTalk_BE.domain.converter;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyResponseDTO;
import DummyTalk.DummyTalk_BE.domain.entity.Dummy;

import java.util.ArrayList;
import java.util.List;

public class DummyConverter {

    public static List<DummyResponseDTO.GetMyDummyListDTO> toGetMyDummyListDTO(List<Dummy> dummyList) {
        List<DummyResponseDTO.GetMyDummyListDTO> dtoList = new ArrayList<>();
        dummyList.stream()
                .map(dummy ->
                    dtoList.add(DummyResponseDTO.GetMyDummyListDTO.builder()
                            .dummyId(dummy.getId())
                            .title(dummy.getTitle())
                            .content(dummy.getContent())
                            .name(dummy.getRarity().getName())
                            .createdAt(dummy.getCreatedAt())
                            .rarityId(dummy.getRarity().getId())
                            .colorCode(dummy.getRarity().getColorCode())
                            .build())
                );
        return dtoList;
    }
}
