package back.kalender.domain.payment.repository;

import back.kalender.domain.payment.entity.PaymentIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentIdempotencyRepository extends JpaRepository<PaymentIdempotency, Long> {
    
    // TTL 체크 포함 조회 (설정된 기간 이내 레코드만 반환)
    @Query("SELECT p FROM PaymentIdempotency p WHERE p.paymentId = :paymentId " +
           "AND p.operation = :operation AND p.idempotencyKey = :idempotencyKey " +
           "AND p.createdAt >= :ttlDate")
    Optional<PaymentIdempotency> findByPaymentIdAndOperationAndIdempotencyKeyWithTtl(
        @Param("paymentId") Long paymentId, 
        @Param("operation") String operation, 
        @Param("idempotencyKey") String idempotencyKey,
        @Param("ttlDate") LocalDateTime ttlDate
    );

    @Modifying
    @Query("DELETE FROM PaymentIdempotency p WHERE p.createdAt < :cutoffDate")
    int deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}
