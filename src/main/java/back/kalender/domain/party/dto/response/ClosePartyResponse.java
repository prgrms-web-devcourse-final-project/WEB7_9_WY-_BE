package back.kalender.domain.party.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파티 모집 마감 응답")
public record ClosePartyResponse(

        @Schema(description = "파티 ID", example = "1")
        Long partyId,

        @Schema(description = "처리 결과 메시지", example = "모집이 마감되었습니다.")
        String message
) {}