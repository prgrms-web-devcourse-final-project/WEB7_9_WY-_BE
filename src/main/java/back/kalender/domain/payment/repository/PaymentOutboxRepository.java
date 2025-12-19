package back.kalender.domain.payment.repository;

import back.kalender.domain.payment.entity.OutboxStatus;
import back.kalender.domain.payment.entity.PaymentOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

// PaymentOutbox 엔티티 리포지토리
public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {

    @Query("SELECT o FROM PaymentOutbox o " +
           "WHERE o.status = :status " +
           "AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now) " +
           "ORDER BY o.createdAt ASC")
    List<PaymentOutbox> findPendingOutboxes(
            @Param("status") OutboxStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}

