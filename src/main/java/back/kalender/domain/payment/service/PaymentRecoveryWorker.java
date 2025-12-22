package back.kalender.domain.payment.service;

import back.kalender.domain.payment.entity.Payment;
import back.kalender.domain.payment.enums.PaymentStatus;
import back.kalender.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// PROCESSING/PROCESSING_TIMEOUT 상태 복구 워커 (단일 인스턴스 가정)
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRecoveryWorker {

    private final PaymentRepository paymentRepository;
    private static final int RECOVERY_THRESHOLD_MINUTES = 5;
    private static final int BATCH_SIZE = 10;

    @Scheduled(fixedDelay = 60000)
    public void recoverStuckProcessingPayments() {
        LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(RECOVERY_THRESHOLD_MINUTES);
        List<Payment> stuckPayments = paymentRepository.findStuckProcessingPayments(thresholdTime);

        if (stuckPayments.isEmpty()) {
            return;
        }

        List<Payment> batchPayments = stuckPayments.stream()
                .limit(BATCH_SIZE)
                .toList();

        log.info("[PaymentRecoveryWorker] 복구 대상 결제 {}건 발견 (배치: {}건)", stuckPayments.size(), batchPayments.size());

        // 개별 트랜잭션으로 처리 (REQUIRES_NEW)
        // 각 결제 복구가 독립적인 트랜잭션으로 실행되어,
        // 하나의 실패가 다른 결제 복구에 영향을 주지 않음
        for (Payment payment : batchPayments) {
            try {
                recoverPayment(payment);
            } catch (Exception e) {
                log.error("[PaymentRecoveryWorker] 복구 실패 - paymentId: {}", payment.getId(), e);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void recoverPayment(Payment payment) {
        if (payment.getStatus() == PaymentStatus.PROCESSING_TIMEOUT) {
            // PROCESSING_TIMEOUT → CREATED 롤백 (재시도 가능)
            log.info("[PaymentRecoveryWorker] PROCESSING_TIMEOUT → CREATED 롤백 - paymentId: {}", payment.getId());
            paymentRepository.updateStatusToCreated(payment.getId());
        } else if (payment.getStatus() == PaymentStatus.PROCESSING) {
            // PROCESSING → CREATED 롤백 (게이트웨이 상태 확인 API 미구현)
            log.warn("[PaymentRecoveryWorker] PROCESSING 상태 복구 - paymentId: {}", payment.getId());
            paymentRepository.updateStatusToCreated(payment.getId());
        }
    }
}
