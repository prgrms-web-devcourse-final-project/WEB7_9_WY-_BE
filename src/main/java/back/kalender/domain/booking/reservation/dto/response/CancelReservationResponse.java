package back.kalender.domain.booking.reservation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "예매 취소 응답")
public record CancelReservationResponse(
        @Schema(description = "예매 ID", example = "1")
        Long reservationId,

        @Schema(description = "예매 상태", example = "CANCELLED")
        String status,

        @Schema(description = "환불 예정 금액", example = "600000")
        Integer refundAmount,

        @Schema(description = "취소 시각", example = "2025-12-20T20:30:00")
        LocalDateTime cancelledAt,

        @Schema(description = "취소된 좌석 수", example = "3")
        Integer cancelledSeatCount
) {
}
