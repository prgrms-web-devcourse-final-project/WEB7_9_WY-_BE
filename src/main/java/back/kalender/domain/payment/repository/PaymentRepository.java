package back.kalender.domain.payment.repository;

import back.kalender.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Payment 엔티티 리포지토리
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 멱등성 체크: userId, orderId, idempotencyKey 조합으로 조회 (보안 강화)
    Optional<Payment> findByUserIdAndOrderIdAndIdempotencyKey(Long userId, String orderId, String idempotencyKey);

    Optional<Payment> findByPaymentKey(String paymentKey);
}

