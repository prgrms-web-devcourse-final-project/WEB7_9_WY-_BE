package back.kalender.domain.payment.dto.response;

import back.kalender.domain.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// 결제 취소 응답 DTO
@Schema(description = "결제 취소 응답")
public record PaymentCancelResponse(
        @Schema(description = "결제 ID", example = "1")
        Long paymentId,

        @Schema(description = "결제 상태", example = "CANCELED")
        PaymentStatus status,

        @Schema(description = "취소 시간", example = "2025-01-01T13:00:00")
        LocalDateTime canceledAt
) {
}
