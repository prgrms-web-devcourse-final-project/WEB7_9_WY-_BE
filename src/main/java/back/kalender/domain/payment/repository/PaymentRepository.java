package back.kalender.domain.payment.repository;

import back.kalender.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Payment 엔티티 리포지토리
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderIdAndIdempotencyKey(String orderId, String idempotencyKey);

    Optional<Payment> findByPaymentKey(String paymentKey);
}

