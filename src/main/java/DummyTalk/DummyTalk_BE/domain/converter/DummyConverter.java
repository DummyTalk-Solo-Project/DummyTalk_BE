package DummyTalk.DummyTalk_BE.domain.converter;

import DummyTalk.DummyTalk_BE.domain.dto.dummy.DummyRespDTO;
import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import DummyTalk.DummyTalk_BE.domain.entity.constant.RarityType;
import DummyTalk.DummyTalk_BE.domain.entity.document.DummyDocument;

import java.util.List;

public class DummyConverter {

    public static List<DummyRespDTO.GetMyDummyDTO> toGetMyDummyListDTO(List<Dummy> dummyList) {
        return dummyList.stream()
                        .map(dummy ->
                                        DummyRespDTO.GetMyDummyDTO.builder()
                                                .dummyId(dummy.getId())
                                                .title(dummy.getTitle())
                                                .content(dummy.getContent())
                                                .name(dummy.getRarity().getName())
                                                .createdAt(dummy.getCreatedAt())
                                                .rarityId(dummy.getRarity().getId())
                                                .colorCode(dummy.getRarity().getColorCode())
                                                .build()
                                ).toList();
    }

    public static List<DummyRespDTO.GetMyDummyDTO> toGetMyDummyDListTO(List<DummyDocument> dummyDocumentList) {
        return dummyDocumentList.stream()
                .map(dq ->
                {
                    DummyRespDTO.GetMyDummyDTO.GetMyDummyDTOBuilder dtoBuilder = DummyRespDTO.GetMyDummyDTO.builder()
                            .dummyId(dq.getId())
                            .title(dq.getTitle())
                            .content(dq.getContent())
                            .name(RarityType.valueOf(dq.getRarityName()))
                            .colorCode(getColorByRarity(RarityType.valueOf(dq.getRarityName())));
//                            .createdAt(dq.getCreatedAt()); // 여기 createdAt은 ES 삽입 기준이므로 잠시 주석 처리
                    return dtoBuilder.build();
                })
                .toList();
    }

    private static String getColorByRarity(RarityType type) {
        return switch (type) {
            case COMMON -> "F4F0E4";
            case RARE -> "44A194";
            case EPIC -> "537D96";
            case SPECIAL -> "EC8F8D";
            case TEST -> "FFFFFF";
        };
    }
}
