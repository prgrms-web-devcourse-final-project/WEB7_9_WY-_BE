package back.kalender.domain.payment.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_idempotency",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_payment_idempotency",
           columnNames = {"paymentId", "operation", "idempotencyKey"}
       ),
       indexes = @Index(
           name = "idx_payment_idempotency_key",
           columnList = "idempotencyKey"
       ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentIdempotency extends BaseEntity {

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false, length = 20)
    private String operation; // PaymentOperation enum의 name() 값 저장

    @Column(nullable = false)
    private String idempotencyKey;

    @Column(columnDefinition = "TEXT")
    private String resultJson; // 재시도 시 반환용 결과 저장

    @Builder
    public PaymentIdempotency(Long paymentId, String operation, String idempotencyKey, String resultJson) {
        this.paymentId = paymentId;
        this.operation = operation;
        this.idempotencyKey = idempotencyKey;
        this.resultJson = resultJson;
    }
}
