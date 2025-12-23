package back.kalender.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// 결제 취소 요청 DTO
@Schema(description = "결제 취소 요청")
public record PaymentCancelRequest(
        @Schema(description = "취소 사유", example = "고객 요청", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "reason is required")
        String reason
) {
}
