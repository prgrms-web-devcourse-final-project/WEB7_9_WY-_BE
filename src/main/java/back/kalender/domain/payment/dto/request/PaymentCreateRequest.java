package back.kalender.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 결제 생성 요청 DTO
@Schema(description = "결제 생성 요청")
@Getter
@Setter
@NoArgsConstructor
public class PaymentCreateRequest {

    @Schema(description = "주문 ID", example = "ORDER-2025-001", required = true)
    @NotBlank(message = "orderId is required")
    private String orderId;

    @Schema(description = "결제 금액", example = "50000", required = true)
    @NotNull(message = "amount is required")
    @Min(value = 1, message = "amount must be greater than 0")
    private Integer amount;

    @Schema(description = "통화", example = "KRW", defaultValue = "KRW")
    private String currency = "KRW";

    @Schema(description = "결제 수단", example = "카드", required = true)
    @NotBlank(message = "method is required")
    private String method;
}
