package back.kalender.domain.party.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AcceptApplicationResponse(
        @Schema(description = "신청자 ID", example = "1")
        Long applicantId,

        @Schema(description = "파티 이름", example = "BTS 공연")
        String partyTitle,

        @Schema(description = "채팅방 ID", example = "1")
        Long chatRoomId
) {
}
