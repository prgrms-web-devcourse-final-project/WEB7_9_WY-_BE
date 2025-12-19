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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// 결제 서비스 - 생성, 조회, 승인, 취소 처리
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentGateway paymentGateway;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentCreateResponse create(PaymentCreateRequest request, String idempotencyKey, Long userId) {
        return paymentRepository.findByUserIdAndOrderIdAndIdempotencyKey(userId, request.getOrderId(), idempotencyKey)
                .map(existingPayment -> {
                    log.info("[Payment] 멱등성: 기존 결제 반환 - paymentId: {}, userId: {}, orderId: {}, idempotencyKey: {}",
                            existingPayment.getId(), userId, existingPayment.getOrderId(), idempotencyKey);
                    return PaymentCreateResponse.builder()
                            .paymentId(existingPayment.getId())
                            .orderId(existingPayment.getOrderId())
                            .amount(existingPayment.getAmount())
                            .status(existingPayment.getStatus())
                            .build();
                })
                .orElseGet(() -> {
                    try {
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
                    } catch (DataIntegrityViolationException e) {
                        // 동시 생성 경쟁: 유니크 제약조건 위반 → 재조회하여 멱등성 보장
                        log.warn("[Payment] 유니크 충돌 발생, 재조회 - userId: {}, orderId: {}, idempotencyKey: {}",
                                userId, request.getOrderId(), idempotencyKey);
                        return paymentRepository.findByUserIdAndOrderIdAndIdempotencyKey(userId, request.getOrderId(), idempotencyKey)
                                .map(existingPayment -> {
                                    log.info("[Payment] 멱등성: 재조회 후 기존 결제 반환 - paymentId: {}", existingPayment.getId());
                                    return PaymentCreateResponse.builder()
                                            .paymentId(existingPayment.getId())
                                            .orderId(existingPayment.getOrderId())
                                            .amount(existingPayment.getAmount())
                                            .status(existingPayment.getStatus())
                                            .build();
                                })
                                .orElseThrow(() -> {
                                    log.error("[Payment] 유니크 충돌 후 재조회 실패 - userId: {}, orderId: {}, idempotencyKey: {}",
                                            userId, request.getOrderId(), idempotencyKey);
                                    return new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR);
                                });
                    }
                });
    }

    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentConfirmResponse confirm(PaymentConfirmRequest request, Long userId) {
        Payment payment = paymentRepository.findByUserIdAndOrderId(userId, request.getOrderId())
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getAmount().equals(request.getAmount())) {
            log.warn("[Payment] 금액 불일치 - paymentId: {}, 저장된 금액: {}, 요청 금액: {}",
                    payment.getId(), payment.getAmount(), request.getAmount());
            throw new ServiceException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        payment.markProcessing();
        PaymentGatewayConfirmResponse gatewayResponse = paymentGateway.confirm(
                request.getPaymentKey(),
                request.getOrderId(),
                request.getAmount()
        );

        if (gatewayResponse.isSuccess()) {
            LocalDateTime approvedAt = LocalDateTime.now();
            payment.approve(gatewayResponse.getPaymentKey(), approvedAt);
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
            saveOutboxEvent(payment.getId(), "PAYMENT_FAILED", createFailedPayload(payment, gatewayResponse));
            
            log.warn("[Payment] 결제 승인 실패 - paymentId: {}, failCode: {}, failMessage: {}",
                    payment.getId(), gatewayResponse.getFailCode(), gatewayResponse.getFailMessage());

            throw new ServiceException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
    }

    @Transactional
    public PaymentCancelResponse cancel(String paymentKey, PaymentCancelRequest request, Long userId) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getUserId().equals(userId)) {
            log.warn("[Payment] 사용자 불일치 - paymentId: {}, 저장된 userId: {}, 요청 userId: {}",
                    payment.getId(), payment.getUserId(), userId);
            throw new ServiceException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        if (payment.getStatus() == PaymentStatus.CANCELED) {
            log.info("[Payment] 이미 취소된 결제 반환 (멱등성) - paymentId: {}, status: {}", payment.getId(), payment.getStatus());
            return PaymentCancelResponse.builder()
                    .paymentId(payment.getId())
                    .status(payment.getStatus())
                    .canceledAt(payment.getCanceledAt())
                    .build();
        }

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
    // 중요: 예외를 잡지 않아서 실패 시 트랜잭션이 롤백되도록 함 (Outbox 패턴의 핵심)
    private void saveOutboxEvent(Long paymentId, String eventType, Map<String, Object> payload) {
        String payloadJson = objectMapper.writeValueAsString(payload);
        PaymentOutbox outbox = PaymentOutbox.builder()
                .paymentId(paymentId)
                .eventType(eventType)
                .payloadJson(payloadJson)
                .build();
        paymentOutboxRepository.save(outbox);
        log.debug("[Payment] Outbox 이벤트 저장 - paymentId: {}, eventType: {}", paymentId, eventType);
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
