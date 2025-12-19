package back.kalender.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 결제 승인 요청 DTO
@Schema(description = "결제 승인 요청")
@Getter
@Setter
@NoArgsConstructor
public class PaymentConfirmRequest {

    @Schema(description = "토스페이먼츠 결제 키", example = "tgen_20250101_abc123", required = true)
    @NotBlank(message = "paymentKey is required")
    private String paymentKey;

    @Schema(description = "예매 ID", example = "123", required = true)
    @NotNull(message = "reservationId is required")
    private Long reservationId;
}
