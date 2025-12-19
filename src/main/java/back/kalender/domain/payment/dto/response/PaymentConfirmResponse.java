package back.kalender.domain.payment.dto.response;

import back.kalender.domain.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 결제 승인 응답 DTO
@Schema(description = "결제 승인 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmResponse {
    
    @Schema(description = "결제 ID", example = "1")
    private Long paymentId;
    
    @Schema(description = "주문 ID", example = "ORDER-2025-001")
    private String orderId;
    
    @Schema(description = "결제 상태", example = "APPROVED")
    private PaymentStatus status;
    
    @Schema(description = "승인 시간", example = "2024-01-01T12:00:00")
    private LocalDateTime approvedAt;
}
