package back.kalender.domain.booking.reservation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record SeatChangesResponse(
        Long currentVersion,
        List<SeatChangeEvent> changes
) {
    @Schema(description = "좌석 상태 변경 이벤트")
    public record SeatChangeEvent(
            @Schema(description = "좌석 ID", example = "1")
            Long seatId,

            @Schema(description = "변경된 상태", example = "HOLD")
            String status,

            @Schema(description = "변경 주체 사용자 ID (해제 시 0)", example = "123")
            Long userId,

            @Schema(description = "변경 버전", example = "5")
            Long version,

            @Schema(description = "변경 시각", example = "2025-12-20T01:23:45")
            String timestamp
    ) {
    }
}
