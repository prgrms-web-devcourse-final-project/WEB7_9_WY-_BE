package back.kalender.domain.booking.reservation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "좌석 선점 실패 응답(충돌/경쟁)")

public record HoldSeatsFailResponse(
        @Schema(description = "예매 ID", example = "123")
        Long reservationId,

        @Schema(description = "좌석표 갱신 필요 여부", example = "true")
        boolean refreshRequired,

        @Schema(description = "충돌 좌석 목록")
        List<ConflictSeat> conflicts,

        @Schema(description = "조회 시각", example = "2026-01-05T14:10:30")
        LocalDateTime updatedAt
) {

    @Schema(description = "충돌 좌석 정보")
    public record ConflictSeat(
            @Schema(description = "좌석 ID", example = "101")
            Long performanceSeatId,

            @Schema(description = "현재 상태", example = "HOLD")
            String currentStatus, // HOLD / SOLD

            @Schema(description = "실패 사유", example = "ALREADY_HELD")
            String reason        // ALREADY_HELD / ALREADY_SOLD / LOCK_FAILED 등
    ) {}

    public static HoldSeatsFailResponse of(Long reservationId, List<ConflictSeat> conflicts) {
        return new HoldSeatsFailResponse(reservationId, true, conflicts, LocalDateTime.now());
    }
}
