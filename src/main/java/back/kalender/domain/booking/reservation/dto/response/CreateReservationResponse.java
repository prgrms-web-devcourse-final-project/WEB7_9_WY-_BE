package back.kalender.domain.booking.reservation.dto.response;

import back.kalender.domain.booking.reservation.entity.Reservation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "예매 세션 생성 응답")
public record CreateReservationResponse(
        @Schema(description = "예매 ID", example = "123")
        Long reservationId,

        @Schema(description = "예매 상태", example = "HOLD")
        String status,

        @Schema(description = "세션 만료 시각", example = "2026-01-05T14:15:00")
        LocalDateTime expiresAt,

        @Schema(description = "남은 시간(초)", example = "300")
        Long remainingSeconds
) {
    public static CreateReservationResponse from(
            Reservation reservation,
            Long remainingSeconds
    ) {
        return new CreateReservationResponse(
                reservation.getId(),
                reservation.getStatus().name(),
                reservation.getExpiresAt(),
                remainingSeconds
        );
    }
}
