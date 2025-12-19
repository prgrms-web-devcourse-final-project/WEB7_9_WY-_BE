package back.kalender.domain.payment.dto.response;

import back.kalender.domain.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 결제 생성 응답 DTO
@Schema(description = "결제 생성 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateResponse {
    
    @Schema(description = "결제 ID", example = "1")
    private Long paymentId;
    
    @Schema(description = "예매 ID", example = "123")
    private Long reservationId;
    
    @Schema(description = "결제 금액", example = "50000")
    private Integer amount;
    
    @Schema(description = "결제 상태", example = "CREATED")
    private PaymentStatus status;
}
