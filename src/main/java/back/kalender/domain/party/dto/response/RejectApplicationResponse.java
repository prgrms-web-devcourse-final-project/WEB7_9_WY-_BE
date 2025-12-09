package back.kalender.domain.party.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record RejectApplicationResponse(
        @Schema(description = "신청자 ID", example = "1")
        long applicantId,

        @Schema(description = "파티 이름", example = "BTS 공연")
        String partyName,

        @Schema(description = "채팅방 ID", example = "1")
        long chatRoomId
) {
}
