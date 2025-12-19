package back.kalender.domain.payment.service;

import back.kalender.domain.payment.dto.request.PaymentCancelRequest;
import back.kalender.domain.payment.dto.request.PaymentConfirmRequest;
import back.kalender.domain.payment.dto.request.PaymentCreateRequest;
import back.kalender.domain.payment.dto.response.*;
import back.kalender.domain.payment.entity.Payment;
import back.kalender.domain.payment.entity.PaymentOutbox;
import back.kalender.domain.payment.entity.PaymentProvider;
import back.kalender.domain.payment.entity.PaymentStatus;
import back.kalender.domain.payment.repository.PaymentOutboxRepository;
import back.kalender.domain.payment.repository.PaymentRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// 결제 서비스 - 생성, 조회, 승인, 취소 처리
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션, 쓰기 작업은 @Transactional로 오버라이드
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentGateway paymentGateway;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentCreateResponse create(PaymentCreateRequest request, String idempotencyKey, Long userId) {
        // 멱등성 보장: 같은 (orderId, idempotencyKey) 조합으로 이미 결제가 있으면 기존 결제 반환
        return paymentRepository.findByOrderIdAndIdempotencyKey(request.getOrderId(), idempotencyKey)
                .map(existingPayment -> {
                    log.info("[Payment] 멱등성: 기존 결제 반환 - paymentId: {}, orderId: {}, idempotencyKey: {}",
                            existingPayment.getId(), existingPayment.getOrderId(), idempotencyKey);
                    return PaymentCreateResponse.builder()
                            .paymentId(existingPayment.getId())
                            .orderId(existingPayment.getOrderId())
                            .amount(existingPayment.getAmount())
                            .status(existingPayment.getStatus())
                            .build();
                })
                .orElseGet(() -> {
                    Payment payment = Payment.builder()
                            .orderId(request.getOrderId())
                            .userId(userId)
                            .provider(PaymentProvider.TOSS)
                            .idempotencyKey(idempotencyKey)
                            .amount(request.getAmount())
                            .currency(request.getCurrency())
                            .method(request.getMethod())
                            .build();

                    Payment savedPayment = paymentRepository.save(payment);
                    log.info("[Payment] 결제 생성 완료 - paymentId: {}, orderId: {}, amount: {}",
                            savedPayment.getId(), savedPayment.getOrderId(), savedPayment.getAmount());

                    return PaymentCreateResponse.builder()
                            .paymentId(savedPayment.getId())
                            .orderId(savedPayment.getOrderId())
                            .amount(savedPayment.getAmount())
                            .status(savedPayment.getStatus())
                            .build();
                });
    }

    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentConfirmResponse confirm(PaymentConfirmRequest request) {
        Payment payment = paymentRepository.findByPaymentKey(request.getPaymentKey())
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        // 보안 검증: 저장된 금액과 요청 금액이 일치하는지 확인 (중간 변조 방지)
        if (!payment.getAmount().equals(request.getAmount())) {
            log.warn("[Payment] 금액 불일치 - paymentId: {}, 저장된 금액: {}, 요청 금액: {}",
                    payment.getId(), payment.getAmount(), request.getAmount());
            throw new ServiceException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 보안 검증: 저장된 주문 ID와 요청 주문 ID가 일치하는지 확인
        if (!payment.getOrderId().equals(request.getOrderId())) {
            log.warn("[Payment] 주문 ID 불일치 - paymentId: {}, 저장된 주문 ID: {}, 요청 주문 ID: {}",
                    payment.getId(), payment.getOrderId(), request.getOrderId());
            throw new ServiceException(ErrorCode.PAYMENT_ORDER_ID_MISMATCH);
        }

        payment.markProcessing(); // 상태를 PROCESSING으로 변경 (게이트웨이 호출 전)
        PaymentGatewayConfirmResponse gatewayResponse = paymentGateway.confirm(
                request.getPaymentKey(),
                request.getOrderId(),
                request.getAmount()
        );

        if (gatewayResponse.isSuccess()) {
            LocalDateTime approvedAt = LocalDateTime.now();
            payment.approve(gatewayResponse.getPaymentKey(), approvedAt);
            // Outbox 패턴: 결제 승인 이벤트를 DB에 저장 (같은 트랜잭션 내에서)
            saveOutboxEvent(payment.getId(), "PAYMENT_APPROVED", createApprovedPayload(payment, approvedAt));
            
            log.info("[Payment] 결제 승인 완료 - paymentId: {}, paymentKey: {}",
                    payment.getId(), gatewayResponse.getPaymentKey());

            return PaymentConfirmResponse.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .status(payment.getStatus())
                    .approvedAt(payment.getApprovedAt())
                    .build();
        } else {
            payment.fail(gatewayResponse.getFailCode(), gatewayResponse.getFailMessage());
            // Outbox 패턴: 결제 실패 이벤트를 DB에 저장
            saveOutboxEvent(payment.getId(), "PAYMENT_FAILED", createFailedPayload(payment, gatewayResponse));
            
            log.warn("[Payment] 결제 승인 실패 - paymentId: {}, failCode: {}, failMessage: {}",
                    payment.getId(), gatewayResponse.getFailCode(), gatewayResponse.getFailMessage());

            throw new ServiceException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
    }

    @Transactional
    public PaymentCancelResponse cancel(String paymentKey, PaymentCancelRequest request) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        // 멱등성: 이미 취소된 결제는 중복 취소 방지
        if (payment.getStatus() == PaymentStatus.CANCELED) {
            log.warn("[Payment] 이미 취소된 결제 - paymentId: {}, status: {}", payment.getId(), payment.getStatus());
            throw new ServiceException(ErrorCode.PAYMENT_ALREADY_CANCELED);
        }
        
        // 상태 검증: APPROVED 상태인 결제만 취소 가능
        if (payment.getStatus() != PaymentStatus.APPROVED) {
            log.warn("[Payment] 취소 불가능한 상태 - paymentId: {}, status: {}", payment.getId(), payment.getStatus());
            throw new ServiceException(ErrorCode.PAYMENT_CANNOT_CANCEL);
        }

        PaymentGatewayCancelResponse gatewayResponse = paymentGateway.cancel(
                paymentKey,
                request.getReason()
        );

        if (gatewayResponse.isSuccess()) {
            LocalDateTime canceledAt = LocalDateTime.now();
            payment.cancel(canceledAt);
            // Outbox 패턴: 결제 취소 이벤트를 DB에 저장
            saveOutboxEvent(payment.getId(), "PAYMENT_CANCELED", createCanceledPayload(payment, request.getReason(), canceledAt));
            
            log.info("[Payment] 결제 취소 완료 - paymentId: {}, paymentKey: {}, reason: {}",
                    payment.getId(), paymentKey, request.getReason());

            return PaymentCancelResponse.builder()
                    .paymentId(payment.getId())
                    .status(payment.getStatus())
                    .canceledAt(payment.getCanceledAt())
                    .build();
        } else {
            log.warn("[Payment] 결제 취소 실패 - paymentId: {}, failCode: {}, failMessage: {}",
                    payment.getId(), gatewayResponse.getFailCode(), gatewayResponse.getFailMessage());
            throw new ServiceException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
    }

    // Outbox 패턴: 이벤트를 DB에 저장하고, 별도 워커가 MQ로 발행 (트랜잭션 안전성 보장)
    private void saveOutboxEvent(Long paymentId, String eventType, Map<String, Object> payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            PaymentOutbox outbox = PaymentOutbox.builder()
                    .paymentId(paymentId)
                    .eventType(eventType)
                    .payloadJson(payloadJson)
                    .build();
            paymentOutboxRepository.save(outbox);
            log.debug("[Payment] Outbox 이벤트 저장 - paymentId: {}, eventType: {}", paymentId, eventType);
        } catch (Exception e) {
            // Outbox 저장 실패해도 결제 처리는 계속 진행 (로깅만)
            log.error("[Payment] Outbox 이벤트 저장 실패 - paymentId: {}, eventType: {}", paymentId, eventType, e);
        }
    }

    private Map<String, Object> createApprovedPayload(Payment payment, LocalDateTime approvedAt) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", payment.getId());
        payload.put("orderId", payment.getOrderId());
        payload.put("userId", payment.getUserId());
        payload.put("amount", payment.getAmount());
        payload.put("paymentKey", payment.getPaymentKey());
        payload.put("approvedAt", approvedAt.toString());
        return payload;
    }

    private Map<String, Object> createFailedPayload(Payment payment, PaymentGatewayConfirmResponse gatewayResponse) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", payment.getId());
        payload.put("orderId", payment.getOrderId());
        payload.put("userId", payment.getUserId());
        payload.put("amount", payment.getAmount());
        payload.put("failCode", gatewayResponse.getFailCode());
        payload.put("failMessage", gatewayResponse.getFailMessage());
        return payload;
    }

    private Map<String, Object> createCanceledPayload(Payment payment, String reason, LocalDateTime canceledAt) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", payment.getId());
        payload.put("orderId", payment.getOrderId());
        payload.put("userId", payment.getUserId());
        payload.put("amount", payment.getAmount());
        payload.put("paymentKey", payment.getPaymentKey());
        payload.put("cancelReason", reason);
        payload.put("canceledAt", canceledAt.toString());
        return payload;
    }
}
