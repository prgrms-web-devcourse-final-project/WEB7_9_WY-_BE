package back.kalender.domain.booking.reservation.dto.response;

import back.kalender.domain.booking.reservation.entity.Reservation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "배송/수령 정보 저장 응답")
public record UpdateDeliveryInfoResponse(
        @Schema(description = "예매 ID", example = "123")
        Long reservationId,

        @Schema(description = "수령 방법", example = "DELIVERY")
        String deliveryMethod,

        @Schema(description = "저장 완료 시각", example = "2026-01-05T14:12:00")
        LocalDateTime updatedAt,

        @Schema(description = "세션 만료 시각", example = "2026-01-05T14:15:00")
        LocalDateTime expiresAt,

        @Schema(description = "남은 시간(초)", example = "180")
        Long remainingSeconds
) {
    public static UpdateDeliveryInfoResponse from(Reservation reservation) {
        long remainingSeconds = java.time.Duration.between(
                LocalDateTime.now(),
                reservation.getExpiresAt()
        ).getSeconds();

        return new UpdateDeliveryInfoResponse(
                reservation.getId(),
                reservation.getDeliveryMethod(),
                reservation.getUpdatedAt(),
                reservation.getExpiresAt(),
                Math.max(0, remainingSeconds)
        );
    }
}
