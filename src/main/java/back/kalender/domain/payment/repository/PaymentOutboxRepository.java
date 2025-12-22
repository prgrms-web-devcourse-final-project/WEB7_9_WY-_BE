package back.kalender.domain.payment.repository;

import back.kalender.domain.payment.entity.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// PaymentOutbox 엔티티 리포지토리
public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long>, PaymentOutboxRepositoryCustom {

    // 클레임한 ID 목록으로 조회
    List<PaymentOutbox> findAllByIdIn(List<Long> ids);
}

