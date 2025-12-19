package back.kalender.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "예매 ID", example = "123", required = true)
    @NotNull(message = "reservationId is required")
    private Long reservationId;

    @Schema(description = "통화", example = "KRW", defaultValue = "KRW")
    private String currency = "KRW";

    @Schema(description = "결제 수단", example = "카드", required = true)
    @NotBlank(message = "method is required")
    private String method;
}
