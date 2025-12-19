package back.kalender.domain.payment.service;

import back.kalender.domain.payment.entity.OutboxStatus;
import back.kalender.domain.payment.entity.PaymentOutbox;
import back.kalender.domain.payment.repository.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Outbox 이벤트를 주기적으로 MQ로 발행하는 스케줄러
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker {

    private final PaymentOutboxRepository paymentOutboxRepository;
    
    // 배치 처리 설정
    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY_COUNT = 5;
    private static final int EXPONENTIAL_BACKOFF_BASE = 2;
    private static final long SCHEDULE_DELAY_MS = 10_000L;

    @Scheduled(fixedDelay = 10000) // SCHEDULE_DELAY_MS와 동일한 값 (10초마다 실행)
    @Transactional
    public void processPendingOutboxes() {
        // PENDING 상태이고 재시도 시간이 지난 이벤트를 조회 (최대 BATCH_SIZE개)
        LocalDateTime now = LocalDateTime.now();
        List<PaymentOutbox> pendingOutboxes = paymentOutboxRepository.findPendingOutboxes(
                OutboxStatus.PENDING,
                now,
                PageRequest.of(0, BATCH_SIZE)
        );

        if (pendingOutboxes.isEmpty()) {
            return;
        }

        log.info("[OutboxWorker] PENDING 이벤트 {}개 처리 시작", pendingOutboxes.size());

        for (PaymentOutbox outbox : pendingOutboxes) {
            try {
                publishToMQ(outbox);
                outbox.markSent();
                log.debug("[OutboxWorker] 이벤트 발행 성공 - outboxId: {}, eventType: {}", 
                        outbox.getId(), outbox.getEventType());
            } catch (Exception e) {
                handlePublishFailure(outbox, e);
            }
        }
    }

    // TODO: 실제 Kafka 또는 RabbitMQ 연동 구현
    private void publishToMQ(PaymentOutbox outbox) {
        log.debug("[OutboxWorker] MQ 발행 (stub) - outboxId: {}, eventType: {}, payload: {}",
                outbox.getId(), outbox.getEventType(), outbox.getPayloadJson());
    }

    private void handlePublishFailure(PaymentOutbox outbox, Exception e) {
        int currentRetryCount = outbox.getRetryCount();
        
        if (currentRetryCount >= MAX_RETRY_COUNT) {
            log.error("[OutboxWorker] 최대 재시도 횟수 초과 - outboxId: {}, retryCount: {}, eventType: {}",
                    outbox.getId(), currentRetryCount, outbox.getEventType(), e);
            return;
        }

        // 재시도 횟수에 따라 대기 시간 증가 (1분, 2분, 4분, 8분, 16분...)
        long backoffMinutes = (long) Math.pow(EXPONENTIAL_BACKOFF_BASE, currentRetryCount);
        LocalDateTime nextRetryAt = LocalDateTime.now().plusMinutes(backoffMinutes);
        outbox.markFailed(nextRetryAt);
        
        log.warn("[OutboxWorker] 이벤트 발행 실패, 재시도 예약 - outboxId: {}, retryCount: {}, nextRetryAt: {}",
                outbox.getId(), currentRetryCount + 1, nextRetryAt, e);
    }
}
