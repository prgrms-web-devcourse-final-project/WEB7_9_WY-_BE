package back.kalender.domain.payment.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PaymentOutboxRepositoryImpl implements PaymentOutboxRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public List<Long> claimPendingOutboxes(int limit) {
        // 선점 업데이트: PENDING/FAILED → PROCESSING으로 변경하여 클레임한 ID 목록 반환
        // FOR UPDATE SKIP LOCKED -> 이미 다른 트랜잭션이 잠근 row는 건너뜀
        // RETURNING id -> 업데이트된 row의 id를 반환
        String sql = "UPDATE payment_outbox o " +
                "SET status = 'PROCESSING' " +
                "FROM (" +
                "  SELECT id FROM payment_outbox " +
                "  WHERE (status = 'PENDING' " +
                "    OR (status = 'FAILED' AND next_retry_at <= CURRENT_TIMESTAMP)) " +
                "  ORDER BY created_at ASC " +
                "  LIMIT :limit " +
                "  FOR UPDATE SKIP LOCKED" +
                ") target " +
                "WHERE o.id = target.id " +
                "RETURNING o.id";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object> resultList = query.getResultList();
        
        // Object를 Long으로 변환
        return resultList.stream()
                .map(id -> ((Number) id).longValue())
                .collect(Collectors.toList());
    }
}
