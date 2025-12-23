package back.kalender.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 결제 승인 요청 DTO
@Schema(description = "결제 승인 요청")
public record PaymentConfirmRequest(
        @Schema(description = "토스페이먼츠 결제 키", example = "tgen_20250101_abc123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "paymentKey is required")
        String paymentKey,

        @Schema(description = "예매 ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "reservationId is required")
        Long reservationId
) {
}
