package back.kalender.domain.payment.entity;

import back.kalender.domain.payment.enums.PaymentProvider;
import back.kalender.domain.payment.enums.PaymentStatus;
import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 결제 엔티티
@Entity
    @Table(name = "payments", indexes = {
    @Index(name = "idx_payment_user_reservation_idempotency", columnList = "userId,reservationId,idempotencyKey"), // 멱등성 조회용
    @Index(name = "idx_payment_user_reservation", columnList = "userId,reservationId") // confirm() 조회용
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_payment_user_reservation_idempotency", columnNames = {"userId", "reservationId", "idempotencyKey"}), // 멱등성 보장
    @UniqueConstraint(name = "uk_payment_payment_key", columnNames = "paymentKey") // 토스페이먼츠 결제 키 중복 방지
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(nullable = false)
    private Long userId; // 멱등성 체크에 포함되어 필수값으로 변경

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider provider;

    @Column(nullable = false)
    private String idempotencyKey; // 멱등성 보장을 위한 키 (같은 요청 중복 방지)

    @Column(unique = true, nullable = true)
    private String paymentKey;

    @Column(nullable = true)
    private String orderId; // 토스페이먼츠 주문 ID (결제창에서 사용한 orderId)

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = true)
    private String failCode;

    @Column(nullable = true)
    private String failMessage;

    @Column(nullable = true)
    private LocalDateTime approvedAt;

    @Column(nullable = true)
    private LocalDateTime canceledAt;

    @Builder
    public Payment(Long reservationId, Long userId, PaymentProvider provider, String idempotencyKey,
                   Integer amount, String currency, String method) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.provider = provider;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.currency = currency;
        this.method = method;
        this.status = PaymentStatus.CREATED;
    }

    // orderId 설정 메서드
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}

