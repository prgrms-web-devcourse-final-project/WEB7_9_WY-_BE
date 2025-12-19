package back.kalender.domain.payment.repository;

import back.kalender.domain.payment.entity.PaymentIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentIdempotencyRepository extends JpaRepository<PaymentIdempotency, Long> {
    
    Optional<PaymentIdempotency> findByPaymentIdAndOperationAndIdempotencyKey(
        Long paymentId, String operation, String idempotencyKey
    );
}
