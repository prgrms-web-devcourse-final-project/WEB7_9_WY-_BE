package back.kalender.domain.payment.service;

import back.kalender.domain.booking.reservation.service.ReservationService;
import back.kalender.domain.payment.constants.PaymentEventType;
import back.kalender.domain.payment.entity.PaymentOutbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;

// Outbox 이벤트 개별 처리 컴포넌트 (REQUIRES_NEW 트랜잭션)
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private static final int MAX_RETRY_COUNT = 3;
    private static final int EXPONENTIAL_BACKOFF_BASE = 2;

    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;
    private final OutboxEventService outboxEventService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOutbox(PaymentOutbox outbox) {
        try {
            // 좌석 SOLD 재처리 이벤트는 직접 처리
            if (PaymentEventType.SEAT_SOLD_RETRY.equals(outbox.getEventType())) {
                retrySeatSold(outbox);
            } else {
                // 기존 MQ 발행 로직
                publishToMQ(outbox);
            }
            outbox.markSent();
            log.info("[OutboxProcessor] 이벤트 처리 성공 - outboxId: {}, eventType: {}", 
                    outbox.getId(), outbox.getEventType());
        } catch (Exception e) {
            handlePublishFailure(outbox, e);
        }
    }

    /**
     * 좌석 SOLD 재처리 로직
     */
    private void retrySeatSold(PaymentOutbox outbox) {
        try {
            // Payload 파싱
            Map<String, Object> payload = objectMapper.readValue(
                    outbox.getPayloadJson(), 
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
            );
            
            Long scheduleId = ((Number) payload.get("scheduleId")).longValue();
            Long reservationId = ((Number) payload.get("reservationId")).longValue();
            
            log.info("[OutboxProcessor] 좌석 SOLD 재처리 시작 - outboxId: {}, scheduleId: {}, reservationId: {}", 
                    outbox.getId(), scheduleId, reservationId);
            
            // 좌석 SOLD 처리
            reservationService.markSeatsAsSold(scheduleId, reservationId);
            
            log.info("[OutboxProcessor] 좌석 SOLD 재처리 완료 - outboxId: {}, scheduleId: {}, reservationId: {}", 
                    outbox.getId(), scheduleId, reservationId);
        } catch (Exception e) {
            log.error("[OutboxProcessor] 좌석 SOLD 재처리 실패 - outboxId: {}, payload: {}", 
                    outbox.getId(), outbox.getPayloadJson(), e);
            throw new RuntimeException("좌석 SOLD 재처리 실패", e);
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
            
            // 좌석 SOLD 재처리 실패인 경우 실패 알림 이벤트 저장 및 상세 로깅
            if (PaymentEventType.SEAT_SOLD_RETRY.equals(outbox.getEventType())) {
                handleSeatSoldAbandoned(outbox, e, currentRetryCount);
            } else {
                log.error("[OutboxProcessor] 최대 재시도 횟수 초과, 포기 - outboxId: {}, retryCount: {}, eventType: {}",
                        outbox.getId(), currentRetryCount, outbox.getEventType(), e);
            }
            return;
        }

        // 지수 백오프: 재시도 횟수에 따라 대기 시간 증가
        long backoffMinutes = (long) Math.pow(EXPONENTIAL_BACKOFF_BASE, currentRetryCount);
        LocalDateTime nextRetryAt = LocalDateTime.now().plusMinutes(backoffMinutes);
        outbox.markFailed(nextRetryAt);
        
        // 좌석 SOLD 재처리인 경우 더 상세한 로깅
        if (PaymentEventType.SEAT_SOLD_RETRY.equals(outbox.getEventType())) {
            try {
                Map<String, Object> payload = objectMapper.readValue(
                        outbox.getPayloadJson(),
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
                );
                log.warn("[OutboxProcessor] 좌석 SOLD 재처리 실패, 재시도 예약 - " +
                        "outboxId: {}, paymentId: {}, reservationId: {}, scheduleId: {}, " +
                        "retryCount: {}/{}, nextRetryAt: {}",
                        outbox.getId(),
                        payload.get("paymentId"),
                        payload.get("reservationId"),
                        payload.get("scheduleId"),
                        currentRetryCount + 1, MAX_RETRY_COUNT, nextRetryAt, e);
            } catch (Exception parseException) {
                log.warn("[OutboxProcessor] 좌석 SOLD 재처리 실패, 재시도 예약 - " +
                        "outboxId: {}, paymentId: {}, retryCount: {}/{}, nextRetryAt: {}",
                        outbox.getId(), outbox.getPaymentId(), 
                        currentRetryCount + 1, MAX_RETRY_COUNT, nextRetryAt, e);
            }
        } else {
            log.warn("[OutboxProcessor] 이벤트 발행 실패, 재시도 예약 - outboxId: {}, retryCount: {}, nextRetryAt: {}",
                    outbox.getId(), currentRetryCount + 1, nextRetryAt, e);
        }
    }

    /**
     * 좌석 SOLD 재처리 최대 재시도 초과 시 처리
     * - 실패 알림 이벤트를 Outbox에 저장하여 관리자 알림/모니터링 가능하도록 함
     * - 상세 로깅으로 문제 추적 가능
     */
    private void handleSeatSoldAbandoned(PaymentOutbox outbox, Exception e, int retryCount) {
        try {
            // Payload 파싱
            Map<String, Object> payload = objectMapper.readValue(
                    outbox.getPayloadJson(),
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
            );
            
            Long paymentId = ((Number) payload.get("paymentId")).longValue();
            Long reservationId = ((Number) payload.get("reservationId")).longValue();
            Long scheduleId = ((Number) payload.get("scheduleId")).longValue();
            
            // 실패 알림 이벤트 저장 (관리자 알림/모니터링용)
            Map<String, Object> failurePayload = new java.util.HashMap<>();
            failurePayload.put("paymentId", paymentId);
            failurePayload.put("reservationId", reservationId);
            failurePayload.put("scheduleId", scheduleId);
            failurePayload.put("originalOutboxId", outbox.getId());
            failurePayload.put("retryCount", retryCount);
            failurePayload.put("failedAt", LocalDateTime.now().toString());
            failurePayload.put("errorMessage", e.getMessage());
            failurePayload.put("errorType", e.getClass().getName());
            
            try {
                outboxEventService.saveOutboxEvent(
                        paymentId,
                        PaymentEventType.SEAT_SOLD_FAILED,
                        failurePayload
                );
                log.info("[OutboxProcessor] 좌석 SOLD 실패 알림 이벤트 저장 완료 - paymentId: {}, reservationId: {}",
                        paymentId, reservationId);
            } catch (Exception saveException) {
                log.error("[OutboxProcessor] 좌석 SOLD 실패 알림 이벤트 저장 실패 - paymentId: {}, reservationId: {}",
                        paymentId, reservationId, saveException);
            }
            
            // 상세 에러 로깅
            log.error("[OutboxProcessor] ⚠️⚠️⚠️ 좌석 SOLD 재처리 최대 재시도 초과 (ABANDONED) ⚠️⚠️⚠️\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "🚨 심각: 결제는 APPROVED 상태인데 좌석이 HOLD로 남아있을 수 있습니다!\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "📋 상세 정보:\n" +
                    "   - Outbox ID: {}\n" +
                    "   - Payment ID: {}\n" +
                    "   - Reservation ID: {}\n" +
                    "   - Schedule ID: {}\n" +
                    "   - 재시도 횟수: {}/{}\n" +
                    "   - 실패 시간: {}\n" +
                    "   - 에러 타입: {}\n" +
                    "   - 에러 메시지: {}\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "🔧 조치 필요:\n" +
                    "   1. Payment ID {}의 결제 상태 확인\n" +
                    "   2. Reservation ID {}의 좌석 상태 확인\n" +
                    "   3. 수동으로 좌석 SOLD 처리 또는 결제 취소 처리\n" +
                    "   4. SEAT_SOLD_FAILED 이벤트를 모니터링하여 알림 설정\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    outbox.getId(),
                    paymentId,
                    reservationId,
                    scheduleId,
                    retryCount, MAX_RETRY_COUNT,
                    LocalDateTime.now(),
                    e.getClass().getName(),
                    e.getMessage(),
                    paymentId,
                    reservationId,
                    e);
                    
        } catch (Exception parseException) {
            log.error("[OutboxProcessor] ⚠️ 좌석 SOLD 재처리 최대 재시도 초과 (ABANDONED) - " +
                    "outboxId: {}, paymentId: {}, retryCount: {}, " +
                    "⚠️ 결제는 APPROVED 상태인데 좌석이 HOLD로 남아있을 수 있습니다. 수동 개입 필요! " +
                    "(Payload 파싱 실패)",
                    outbox.getId(), outbox.getPaymentId(), retryCount, parseException);
        }
    }
}
