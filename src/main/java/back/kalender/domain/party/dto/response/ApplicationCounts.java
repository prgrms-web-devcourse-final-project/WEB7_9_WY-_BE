package back.kalender.domain.party.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파티 신청 통계")
public record ApplicationCounts(

        @Schema(description = "대기 중인 신청 수", example = "3")
        int pending,

        @Schema(description = "승인된 신청 수", example = "5")
        int approved,

        @Schema(description = "거절된 신청 수", example = "2")
        int rejected
) {}