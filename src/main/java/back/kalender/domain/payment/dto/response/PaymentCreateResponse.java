package back.kalender.domain.payment.dto.response;

import back.kalender.domain.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

// 결제 생성 응답 DTO
@Schema(description = "결제 생성 응답")
public record PaymentCreateResponse(
        @Schema(description = "결제 ID", example = "1")
        Long paymentId,

        @Schema(description = "예매 ID", example = "123")
        Long reservationId,

        @Schema(description = "결제 금액", example = "50000")
        Integer amount,

        @Schema(description = "결제 상태", example = "CREATED")
        PaymentStatus status
) {
}
