package back.kalender.domain.payment.mapper;

import back.kalender.domain.payment.dto.request.PaymentCreateRequest;
import back.kalender.domain.payment.dto.response.PaymentCancelResponse;
import back.kalender.domain.payment.dto.response.PaymentConfirmResponse;
import back.kalender.domain.payment.dto.response.PaymentCreateResponse;
import back.kalender.domain.payment.entity.Payment;
import back.kalender.domain.payment.enums.PaymentProvider;

// Payment 엔티티 생성 및 Response DTO 변환 매퍼
public class PaymentMapper {

    private PaymentMapper() {
        // 인스턴스화 방지
    }

    // reservationId + userId + idempotencyKey + amount + currency + method → Payment 엔티티 생성
    public static Payment create(Long reservationId, Long userId, String idempotencyKey, Integer amount, String currency, String method) {
        return Payment.builder()
                .reservationId(reservationId)
                .userId(userId)
                .provider(PaymentProvider.TOSS)
                .idempotencyKey(idempotencyKey)
                .amount(amount)
                .currency(currency)
                .method(method)
                .build();
    }

    // Payment -> PaymentCreateResponse 변환
    public static PaymentCreateResponse toCreateResponse(Payment payment) {
        return PaymentCreateResponse.builder()
                .paymentId(payment.getId())
                .reservationId(payment.getReservationId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .build();
    }

    // Payment -> PaymentConfirmResponse 변환
    public static PaymentConfirmResponse toConfirmResponse(Payment payment) {
        return PaymentConfirmResponse.builder()
                .paymentId(payment.getId())
                .reservationId(payment.getReservationId())
                .status(payment.getStatus())
                .approvedAt(payment.getApprovedAt())
                .build();
    }

    // Payment -> PaymentCancelResponse 변환
    public static PaymentCancelResponse toCancelResponse(Payment payment) {
        return PaymentCancelResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .canceledAt(payment.getCanceledAt())
                .build();
    }
}
