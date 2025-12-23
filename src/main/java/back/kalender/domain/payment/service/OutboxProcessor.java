package back.kalender.domain.payment.service;

import back.kalender.domain.payment.entity.PaymentOutbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Outbox 이벤트 개별 처리 컴포넌트 (REQUIRES_NEW 트랜잭션)
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private static final int MAX_RETRY_COUNT = 5;
    private static final int EXPONENTIAL_BACKOFF_BASE = 2;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOutbox(PaymentOutbox outbox) {
        try {
            publishToMQ(outbox);
            outbox.markSent();
            log.debug("[OutboxProcessor] 이벤트 발행 성공 - outboxId: {}, eventType: {}", 
                    outbox.getId(), outbox.getEventType());
        } catch (Exception e) {
            handlePublishFailure(outbox, e);
        }
    }

    // TODO: 실제 Kafka 또는 RabbitMQ 연동 구현
    private void publishToMQ(PaymentOutbox outbox) {
        log.debug("[OutboxProcessor] MQ 발행 (stub) - outboxId: {}, eventType: {}, payload: {}",
                outbox.getId(), outbox.getEventType(), outbox.getPayloadJson());
    }

    private void handlePublishFailure(PaymentOutbox outbox, Exception e) {
        int currentRetryCount = outbox.getRetryCount();
        
        if (currentRetryCount >= MAX_RETRY_COUNT) {
            // 최대 재시도 초과 시 ABANDONED 상태로 변경 (무한 루프 방지)
            outbox.markAbandoned();
            log.error("[OutboxProcessor] 최대 재시도 횟수 초과, 포기 - outboxId: {}, retryCount: {}, eventType: {}",
                    outbox.getId(), currentRetryCount, outbox.getEventType(), e);
            return;
        }

        // 지수 백오프: 재시도 횟수에 따라 대기 시간 증가
        long backoffMinutes = (long) Math.pow(EXPONENTIAL_BACKOFF_BASE, currentRetryCount);
        LocalDateTime nextRetryAt = LocalDateTime.now().plusMinutes(backoffMinutes);
        outbox.markFailed(nextRetryAt);
        
        log.warn("[OutboxProcessor] 이벤트 발행 실패, 재시도 예약 - outboxId: {}, retryCount: {}, nextRetryAt: {}",
                outbox.getId(), currentRetryCount + 1, nextRetryAt, e);
    }
}
