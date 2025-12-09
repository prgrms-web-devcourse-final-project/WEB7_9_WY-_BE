package back.kalender.domain.party.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreatePartyResponse(
        @Schema(description = "파티 ID", example = "1")
        long partyId,

        @Schema(description = "파티장 ID", example = "1")
        long leaderId,

        @Schema(description = "상태", example = "수정 완료")
        String status
) {
}
