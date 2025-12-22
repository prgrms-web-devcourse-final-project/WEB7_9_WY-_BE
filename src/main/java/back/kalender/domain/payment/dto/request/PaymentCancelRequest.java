package back.kalender.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 결제 취소 요청 DTO
@Schema(description = "결제 취소 요청")
@Getter
@Setter
@NoArgsConstructor
public class PaymentCancelRequest {

    @Schema(description = "취소 사유", example = "고객 요청", required = true)
    @NotBlank(message = "reason is required")
    private String reason;
}
