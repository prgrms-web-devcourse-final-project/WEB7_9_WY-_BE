package back.kalender.domain.payment.service;

import back.kalender.domain.payment.entity.PaymentOutbox;
import back.kalender.domain.payment.repository.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

// Outbox 이벤트 저장 서비스 (REQUIRES_NEW 트랜잭션)
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventService {

    private final PaymentOutboxRepository paymentOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOutboxEvent(Long paymentId, String eventType, Map<String, Object> payload) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("[OutboxEvent] JSON 직렬화 실패 - paymentId: {}, eventType: {}", paymentId, eventType, e);
            throw new RuntimeException("Outbox 이벤트 JSON 직렬화 실패", e);
        }
        
        PaymentOutbox outbox = PaymentOutbox.builder()
                .paymentId(paymentId)
                .eventType(eventType)
                .payloadJson(payloadJson)
                .build();
        paymentOutboxRepository.save(outbox);
        log.debug("[OutboxEvent] 이벤트 저장 완료 - paymentId: {}, eventType: {}", paymentId, eventType);
    }
}
