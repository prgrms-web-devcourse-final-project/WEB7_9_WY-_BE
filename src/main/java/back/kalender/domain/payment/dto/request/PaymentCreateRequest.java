package back.kalender.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 결제 생성 요청 DTO
@Schema(description = "결제 생성 요청")
public record PaymentCreateRequest(
        @Schema(description = "예매 ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "reservationId is required")
        Long reservationId,

        @Schema(description = "결제 수단", example = "카드", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "method is required")
        String method,

        @Schema(description = "통화", example = "KRW", defaultValue = "KRW")
        String currency
) {
    public PaymentCreateRequest {
        currency = (currency == null || currency.isBlank()) ? "KRW" : currency;
    }
}
