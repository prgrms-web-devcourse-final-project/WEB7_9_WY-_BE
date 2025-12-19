package back.kalender.domain.payment.repository;

import back.kalender.domain.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Payment 엔티티 리포지토리
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByUserIdAndOrderIdAndIdempotencyKey(Long userId, String orderId, String idempotencyKey);

    // 비관적 락: 상태 전이 동시성 문제 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findByUserIdAndOrderId(
            @Param("userId") Long userId,
            @Param("orderId") String orderId
    );

    // 비관적 락: 상태 전이 동시성 문제 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findByPaymentKey(@Param("paymentKey") String paymentKey);
}

