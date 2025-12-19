package back.kalender.domain.party.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreatePartyResponse(
        @Schema(description = "파티 ID", example = "1")
        Long partyId,

        @Schema(description = "파티장 ID", example = "1")
        Long leaderId,

        @Schema(description = "상태", example = "생성 완료")
        String status
) {
}
