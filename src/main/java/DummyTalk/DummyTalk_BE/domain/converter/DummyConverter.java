package DummyTalk.DummyTalk_BE.domain.converter;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyResponseDTO;
import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.constant.RarityType;
import DummyTalk.DummyTalk_BE.domain.entity.document.DummyDocument;

import java.util.ArrayList;
import java.util.List;

public class DummyConverter {

    public static List<DummyResponseDTO.GetMyDummyDTO> toGetMyDummyListDTO(List<Dummy> dummyList) {
        List<DummyResponseDTO.GetMyDummyDTO> dtoList = new ArrayList<>();
        dummyList.stream()
                .map(dummy ->
                        dtoList.add(DummyResponseDTO.GetMyDummyDTO.builder()
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

    public static List<DummyResponseDTO.GetMyDummyDTO> toGetMyDummyDListTO(List<DummyDocument> dummyDocumentList) {
        return dummyDocumentList.stream()
                .map(dq ->
                {
                    RarityType type = RarityType.valueOf(dq.getRarityName());
                    DummyResponseDTO.GetMyDummyDTO.GetMyDummyDTOBuilder dtoBuilder = DummyResponseDTO.GetMyDummyDTO.builder()
                            .dummyId(dq.getId())
                            .title(dq.getTitle())
                            .content(dq.getContent())
                            .name(type)
                            .createdAt(dq.getCreatedAt());

                    if (type.equals(RarityType.COMMON)) {
                        dtoBuilder.colorCode("COMMON");
                    } else if (type.equals(RarityType.RARE)) {
                        dtoBuilder.colorCode("RARE");
                    } else if (type.equals(RarityType.EPIC)) {
                        dtoBuilder.colorCode("EPIC");
                    } else if (type.equals(RarityType.SPECIAL)) {
                        dtoBuilder.colorCode("SPECIAL");
                    }

                    return dtoBuilder.build();
                })
                .toList();
    }
}
