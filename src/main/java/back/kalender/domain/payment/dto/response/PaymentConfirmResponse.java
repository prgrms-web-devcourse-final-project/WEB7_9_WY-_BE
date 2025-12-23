package back.kalender.domain.payment.dto.response;

import back.kalender.domain.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// 결제 승인 응답 DTO
@Schema(description = "결제 승인 응답")
public record PaymentConfirmResponse(
        @Schema(description = "결제 ID", example = "1")
        Long paymentId,

        @Schema(description = "예매 ID", example = "123")
        Long reservationId,

        @Schema(description = "결제 상태", example = "APPROVED")
        PaymentStatus status,

        @Schema(description = "승인 시간", example = "2024-01-01T12:00:00")
        LocalDateTime approvedAt
) {
}
