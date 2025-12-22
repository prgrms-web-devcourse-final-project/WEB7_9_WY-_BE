package back.kalender.domain.payment.dto.response;

import back.kalender.domain.payment.entity.Payment;
import back.kalender.domain.payment.enums.PaymentProvider;
import back.kalender.domain.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// 결제 조회 응답 DTO
@Schema(description = "결제 조회 응답")
public record PaymentResponse(
        @Schema(description = "결제 ID", example = "1")
        Long paymentId,

        @Schema(description = "예매 ID", example = "123")
        Long reservationId,

        @Schema(description = "사용자 ID", example = "100")
        Long userId,

        @Schema(description = "결제 제공자", example = "TOSS")
        PaymentProvider provider,

        @Schema(description = "토스페이먼츠 결제 키", example = "tgen_20240101_abc123")
        String paymentKey,

        @Schema(description = "결제 금액", example = "50000")
        Integer amount,

        @Schema(description = "통화", example = "KRW")
        String currency,

        @Schema(description = "결제 수단", example = "카드")
        String method,

        @Schema(description = "결제 상태", example = "APPROVED")
        PaymentStatus status,

        @Schema(description = "실패 코드", example = "CARD_AUTH_FAILED")
        String failCode,

        @Schema(description = "실패 메시지", example = "카드 인증에 실패했습니다")
        String failMessage,

        @Schema(description = "승인 시간", example = "2025-01-01T12:00:00")
        LocalDateTime approvedAt,

        @Schema(description = "취소 시간", example = "2025-01-01T13:00:00")
        LocalDateTime canceledAt,

        @Schema(description = "생성 시간", example = "2025-01-01T11:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 시간", example = "2025-01-01T12:00:00")
        LocalDateTime updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getReservationId(),
                payment.getUserId(),
                payment.getProvider(),
                payment.getPaymentKey(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getFailCode(),
                payment.getFailMessage(),
                payment.getApprovedAt(),
                payment.getCanceledAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
