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
    @Index(name = "idx_payment_user_order_idempotency", columnList = "userId,orderId,idempotencyKey"), // 멱등성 조회용
    @Index(name = "idx_payment_user_order", columnList = "userId,orderId") // confirm() 조회용
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_payment_user_order_idempotency", columnNames = {"userId", "orderId", "idempotencyKey"}), // 멱등성 보장
    @UniqueConstraint(name = "uk_payment_payment_key", columnNames = "paymentKey") // 토스페이먼츠 결제 키 중복 방지
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private Long userId; // 멱등성 체크에 포함되어 필수값으로 변경

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider provider;

    @Column(nullable = false)
    private String idempotencyKey; // 멱등성 보장을 위한 키 (같은 요청 중복 방지)

    @Column(unique = true, nullable = true)
    private String paymentKey;

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
    public Payment(String orderId, Long userId, PaymentProvider provider, String idempotencyKey,
                   Integer amount, String currency, String method) {
        this.orderId = orderId;
        this.userId = userId;
        this.provider = provider;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.currency = currency;
        this.method = method;
        this.status = PaymentStatus.CREATED;
    }

    // 상태 전이 메서드들 (참고용)
    // 주의: 현재는 조건부 UPDATE를 사용하므로 이 메서드들은 사용되지 않음
    // 동시성 안전성을 위해 PaymentRepository의 조건부 UPDATE 메서드 사용 권장
    
    @Deprecated
    public void markProcessing() {
        if (this.status != PaymentStatus.CREATED) {
            throw new IllegalStateException("Only CREATED payments can be marked as PROCESSING");
        }
        this.status = PaymentStatus.PROCESSING;
    }

    @Deprecated
    public void approve(String paymentKey, LocalDateTime approvedAt) {
        if (this.status == PaymentStatus.APPROVED) {
            return;
        }
        if (this.status != PaymentStatus.PROCESSING && this.status != PaymentStatus.CREATED) {
            throw new IllegalStateException("Only PROCESSING or CREATED payments can be approved");
        }
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = approvedAt;
    }

    @Deprecated
    public void fail(String code, String message) {
        this.status = PaymentStatus.FAILED;
        this.failCode = code;
        this.failMessage = message;
    }

    @Deprecated
    public void cancel(LocalDateTime canceledAt) {
        if (this.status == PaymentStatus.CANCELED) {
            return;
        }
        if (this.status != PaymentStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED payments can be canceled");
        }
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = canceledAt;
    }
}

