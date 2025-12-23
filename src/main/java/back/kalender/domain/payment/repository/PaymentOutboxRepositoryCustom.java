package back.kalender.domain.payment.repository;

import java.util.List;

public interface PaymentOutboxRepositoryCustom {
    // 선점 업데이트: PENDING/FAILED → PROCESSING으로 변경하여 클레임한 ID 목록 반환
    List<Long> claimPendingOutboxes(int limit);
}
