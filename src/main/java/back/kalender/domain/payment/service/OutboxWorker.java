package back.kalender.domain.payment.service;

import back.kalender.domain.payment.entity.PaymentOutbox;
import back.kalender.domain.payment.repository.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Outbox 이벤트 MQ 발행 스케줄러 (단일 인스턴스 가정)
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker {

    private final PaymentOutboxRepository paymentOutboxRepository;
    private final OutboxProcessor outboxProcessor;
    
    private static final int BATCH_SIZE = 100;

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void processPendingOutboxes() {
        // 선점 업데이트: PENDING/FAILED → PROCESSING으로 클레임
        List<Long> claimedIds = paymentOutboxRepository.claimPendingOutboxes(BATCH_SIZE);

        if (claimedIds.isEmpty()) {
            return;
        }

        List<PaymentOutbox> processingOutboxes = paymentOutboxRepository.findAllByIdIn(claimedIds);
        
        // ID -> 인덱스 매핑 생성 (O(n))
        Map<Long, Integer> idToIndexMap = new HashMap<>();
        for (int i = 0; i < claimedIds.size(); i++) {
            idToIndexMap.put(claimedIds.get(i), i);
        }
        
        // 클레임 순서 유지 정렬 (O(n log n))
        processingOutboxes.sort((o1, o2) -> {
            Integer idx1 = idToIndexMap.get(o1.getId());
            Integer idx2 = idToIndexMap.get(o2.getId());
            return Integer.compare(idx1, idx2);
        });

        log.info("[OutboxWorker] PROCESSING 이벤트 {}개 처리 시작", processingOutboxes.size());

        // 개별 트랜잭션으로 처리
        for (PaymentOutbox outbox : processingOutboxes) {
            outboxProcessor.processOutbox(outbox);
        }
    }
}
