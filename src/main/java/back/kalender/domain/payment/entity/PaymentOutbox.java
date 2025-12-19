package back.kalender.domain.payment.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// Outbox 패턴을 위한 이벤트 저장 엔티티
@Entity
    @Table(name = "payment_outbox", indexes = {
    @Index(name = "idx_outbox_payment_id", columnList = "paymentId"), // 특정 결제의 이벤트 조회용
    @Index(name = "idx_outbox_status_retry", columnList = "status,nextRetryAt") // 워커가 PENDING 이벤트 조회용
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentOutbox extends BaseEntity {

    @Column(columnDefinition = "BINARY(16)", nullable = false, unique = true)
    private UUID eventId; // 이벤트 고유 id (비즈니스용, pk 아님) - 메시지 키/트레이싱에 사용

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private Integer retryCount;

    @Column(nullable = true)
    private LocalDateTime nextRetryAt;

    @Column(nullable = true)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        // 엔티티 저장 전 자동 초기화: eventId, status, retryCount
        if (this.eventId == null) {
            this.eventId = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = OutboxStatus.PENDING;
        }
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
    }

    @Builder
    public PaymentOutbox(Long paymentId, String eventType, String payloadJson) {
        this.paymentId = paymentId;
        this.eventType = eventType;
        this.payloadJson = payloadJson;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
    }

    // PENDING → SENT (MQ 발행 성공 시)
    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    // PENDING → FAILED (MQ 발행 실패 시, 재시도 시간 설정)
    public void markFailed(LocalDateTime nextRetryAt) {
        this.status = OutboxStatus.FAILED;
        this.retryCount++;
        this.nextRetryAt = nextRetryAt;
    }

    // FAILED → PENDING (재시도 시)
    public void resetToPending() {
        this.status = OutboxStatus.PENDING;
    }
}

