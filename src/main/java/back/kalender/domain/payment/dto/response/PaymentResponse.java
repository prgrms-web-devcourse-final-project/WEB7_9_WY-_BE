package back.kalender.domain.payment.dto.response;

import back.kalender.domain.payment.entity.Payment;
import back.kalender.domain.payment.enums.PaymentProvider;
import back.kalender.domain.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 결제 조회 응답 DTO
@Schema(description = "결제 조회 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    
    @Schema(description = "결제 ID", example = "1")
    private Long paymentId;
    
    @Schema(description = "예매 ID", example = "123")
    private Long reservationId;
    
    @Schema(description = "사용자 ID", example = "100")
    private Long userId;
    
    @Schema(description = "결제 제공자", example = "TOSS")
    private PaymentProvider provider;
    
    @Schema(description = "토스페이먼츠 결제 키", example = "tgen_20240101_abc123")
    private String paymentKey;
    
    @Schema(description = "결제 금액", example = "50000")
    private Integer amount;
    
    @Schema(description = "통화", example = "KRW")
    private String currency;
    
    @Schema(description = "결제 수단", example = "카드")
    private String method;
    
    @Schema(description = "결제 상태", example = "APPROVED")
    private PaymentStatus status;
    
    @Schema(description = "실패 코드", example = "CARD_AUTH_FAILED")
    private String failCode;
    
    @Schema(description = "실패 메시지", example = "카드 인증에 실패했습니다")
    private String failMessage;
    
    @Schema(description = "승인 시간", example = "2025-01-01T12:00:00")
    private LocalDateTime approvedAt;
    
    @Schema(description = "취소 시간", example = "2025-01-01T13:00:00")
    private LocalDateTime canceledAt;
    
    @Schema(description = "생성 시간", example = "2025-01-01T11:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정 시간", example = "2025-01-01T12:00:00")
    private LocalDateTime updatedAt;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .reservationId(payment.getReservationId())
                .userId(payment.getUserId())
                .provider(payment.getProvider())
                .paymentKey(payment.getPaymentKey())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .failCode(payment.getFailCode())
                .failMessage(payment.getFailMessage())
                .approvedAt(payment.getApprovedAt())
                .canceledAt(payment.getCanceledAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
