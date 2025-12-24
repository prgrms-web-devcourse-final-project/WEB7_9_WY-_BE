package back.kalender.domain.payment.service;

import back.kalender.domain.payment.dto.request.PaymentCancelRequest;
import back.kalender.domain.payment.dto.request.PaymentConfirmRequest;
import back.kalender.domain.payment.dto.request.PaymentCreateRequest;
import back.kalender.domain.payment.dto.response.*;
import back.kalender.domain.payment.constants.PaymentEventType;
import back.kalender.domain.payment.entity.Payment;
import back.kalender.domain.payment.entity.PaymentIdempotency;
import back.kalender.domain.payment.enums.PaymentOperation;
import back.kalender.domain.payment.enums.PaymentStatus;
import back.kalender.domain.payment.mapper.PaymentMapper;
import back.kalender.domain.payment.repository.PaymentIdempotencyRepository;
import back.kalender.domain.payment.repository.PaymentRepository;
import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservation.entity.ReservationStatus;
import back.kalender.domain.booking.reservation.repository.ReservationRepository;
import back.kalender.domain.booking.reservation.service.ReservationService;
import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
import back.kalender.domain.booking.reservationSeat.repository.ReservationSeatRepository;
import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentIdempotencyRepository paymentIdempotencyRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final PerformanceSeatRepository performanceSeatRepository;
    private final ReservationService reservationService;
    private final PaymentGateway paymentGateway;
    private final ObjectMapper objectMapper;
    private final OutboxEventService outboxEventService;
    
    @Value("${custom.payment.idempotency.ttlDays:7}")
    private int idempotencyTtlDays;

    @Transactional
    public PaymentCreateResponse create(PaymentCreateRequest request, String idempotencyKey, Long userId) {
        // Reservation 조회 및 검증
        Reservation reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new ServiceException(ErrorCode.RESERVATION_NOT_FOUND));
        
        if (!reservation.getUserId().equals(userId)) {
            log.warn("[Payment] 예매 소유자 불일치 - reservationId: {}, 저장된 userId: {}, 요청 userId: {}",
                    request.reservationId(), reservation.getUserId(), userId);
            throw new ServiceException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        
        // 예매 상태 검증: HOLD 상태여야만 결제 생성 가능
        if (reservation.getStatus() != ReservationStatus.HOLD) {
            log.warn("[Payment] 예매 상태가 HOLD가 아님 - reservationId: {}, status: {}", 
                    request.reservationId(), reservation.getStatus());
            throw new ServiceException(ErrorCode.RESERVATION_NOT_HOLD);
        }
        
        // 만료 여부 검증
        if (reservation.isExpired()) {
            log.warn("[Payment] 예매가 만료됨 - reservationId: {}, expiresAt: {}", 
                    request.reservationId(), reservation.getExpiresAt());
            throw new ServiceException(ErrorCode.RESERVATION_EXPIRED);
        }
        
        // 실제로 홀드된 좌석이 있는지 확인
        List<ReservationSeat> reservationSeats = reservationSeatRepository.findByReservationId(request.reservationId());
        if (reservationSeats.isEmpty()) {
            log.warn("[Payment] 홀드된 좌석이 없음 - reservationId: {}", request.reservationId());
            throw new ServiceException(ErrorCode.NO_SEATS_HELD);
        }
        
        // Reservation의 totalAmount 사용 (클라이언트가 보낸 amount 무시)
        Integer amount = reservation.getTotalAmount();
        
        // totalAmount가 0이면 좌석이 홀드되지 않은 상태이므로 결제 생성 불가
        if (amount == null || amount <= 0) {
            log.warn("[Payment] 결제 금액이 0 이하 - reservationId: {}, totalAmount: {}", 
                    request.reservationId(), amount);
            throw new ServiceException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        
        return paymentRepository.findByUserIdAndReservationIdAndIdempotencyKey(userId, request.reservationId(), idempotencyKey)
                .map(existingPayment -> {
                    log.info("[Payment] 멱등성: 기존 결제 반환 - paymentId: {}, userId: {}, reservationId: {}, idempotencyKey: {}",
                            existingPayment.getId(), userId, existingPayment.getReservationId(), idempotencyKey);
                    return PaymentMapper.toCreateResponse(existingPayment);
                })
                .orElseGet(() -> {
                    try {
                        Payment payment = PaymentMapper.create(request.reservationId(), userId, idempotencyKey, amount, request.currency(), request.method());

                        Payment savedPayment = paymentRepository.save(payment);
                        log.info("[Payment] 결제 생성 완료 - paymentId: {}, reservationId: {}, amount: {}",
                                savedPayment.getId(), savedPayment.getReservationId(), savedPayment.getAmount());

                        return PaymentMapper.toCreateResponse(savedPayment);
                    } catch (DataIntegrityViolationException e) {
                        // 동시 생성 경쟁 시 재조회하여 멱등성 보장
                        log.warn("[Payment] 유니크 충돌 발생, 재조회 - userId: {}, reservationId: {}, idempotencyKey: {}",
                                userId, request.reservationId(), idempotencyKey);
                        return paymentRepository.findByUserIdAndReservationIdAndIdempotencyKey(userId, request.reservationId(), idempotencyKey)
                                .map(existingPayment -> {
                                    log.info("[Payment] 멱등성: 재조회 후 기존 결제 반환 - paymentId: {}", existingPayment.getId());
                                    return PaymentMapper.toCreateResponse(existingPayment);
                                })
                                .orElseThrow(() -> {
                                    log.error("[Payment] 유니크 충돌 후 재조회 실패 - userId: {}, reservationId: {}, idempotencyKey: {}",
                                            userId, request.reservationId(), idempotencyKey);
                                    return new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR);
                                });
                    }
                });
    }

    public PaymentResponse getPayment(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
        
        // 권한 검증: 본인 결제만 조회 가능
        if (!payment.getUserId().equals(userId)) {
            log.warn("[Payment] 결제 조회 권한 없음 - paymentId: {}, 저장된 userId: {}, 요청 userId: {}",
                    paymentId, payment.getUserId(), userId);
            throw new ServiceException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentConfirmResponse confirm(PaymentConfirmRequest request, Long userId, String idempotencyKey) {
        Payment payment = paymentRepository.findByUserIdAndReservationId(userId, request.reservationId())
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        // Reservation 조회 및 금액 검증 (클라이언트가 보낸 amount 무시, Reservation의 totalAmount 사용)
        Reservation reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new ServiceException(ErrorCode.RESERVATION_NOT_FOUND));
        
        if (!reservation.getUserId().equals(userId)) {
            log.warn("[Payment] 예매 소유자 불일치 - reservationId: {}, 저장된 userId: {}, 요청 userId: {}",
                    request.reservationId(), reservation.getUserId(), userId);
            throw new ServiceException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        
        // 예매 상태 검증: HOLD 상태여야만 결제 승인 가능
        if (reservation.getStatus() != ReservationStatus.HOLD) {
            log.warn("[Payment] 예매 상태가 HOLD가 아님 - reservationId: {}, status: {}", 
                    request.reservationId(), reservation.getStatus());
            throw new ServiceException(ErrorCode.RESERVATION_NOT_HOLD);
        }
        
        // 만료 여부 검증
        if (reservation.isExpired()) {
            log.warn("[Payment] 예매가 만료됨 - reservationId: {}, expiresAt: {}", 
                    request.reservationId(), reservation.getExpiresAt());
            throw new ServiceException(ErrorCode.RESERVATION_EXPIRED);
        }
        
        // Payment에 저장된 금액과 Reservation의 totalAmount 비교
        if (!payment.getAmount().equals(reservation.getTotalAmount())) {
            log.warn("[Payment] 결제 금액 불일치 - paymentId: {}, paymentAmount: {}, reservationTotalAmount: {}",
                    payment.getId(), payment.getAmount(), reservation.getTotalAmount());
            throw new ServiceException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 이미 APPROVED 상태인 경우: 멱등성 검증 후 반환
        if (payment.getStatus() == PaymentStatus.APPROVED) {
            log.info("[Payment] 이미 승인된 결제 - paymentId: {}, status: {}", 
                    payment.getId(), payment.getStatus());
            
            // 멱등성 검증: TTL 체크 포함 조회
            LocalDateTime ttlDate = LocalDateTime.now().minusDays(idempotencyTtlDays);
            Optional<PaymentIdempotency> existingIdempotency = paymentIdempotencyRepository
                    .findByPaymentIdAndOperationAndIdempotencyKeyWithTtl(
                        payment.getId(), PaymentOperation.CONFIRM.name(), idempotencyKey, ttlDate);
            
            if (existingIdempotency.isPresent()) {
                log.info("[Payment] 멱등성: 기존 승인 결과 반환 - paymentId: {}, idempotencyKey: {}", 
                        payment.getId(), idempotencyKey);
                return parseConfirmResponse(existingIdempotency.get().getResultJson(), payment);
            }
            
            // 멱등성 레코드가 없는 경우: 좌석 SOLD 상태 확인 후 멱등성 저장
            boolean seatSoldSuccess = checkSeatsSoldStatus(reservation.getId());
            
            PaymentConfirmResponse response = PaymentMapper.toConfirmResponse(payment);
            
            if (seatSoldSuccess) {
                // 좌석이 모두 SOLD 상태면 멱등성 저장
                saveIdempotency(payment.getId(), PaymentOperation.CONFIRM, idempotencyKey, response);
                log.info("[Payment] 이미 승인된 결제, 좌석 SOLD 확인 후 멱등성 저장 - paymentId: {}, reservationId: {}", 
                        payment.getId(), reservation.getId());
            } else {
                // 좌석이 HOLD 상태면 좌석 SOLD 재처리 시도
                log.warn("[Payment] 이미 승인된 결제지만 좌석이 HOLD 상태, 재처리 시도 - paymentId: {}, reservationId: {}", 
                        payment.getId(), reservation.getId());
                try {
                    reservationService.markSeatsAsSold(
                            reservation.getPerformanceScheduleId(),
                            reservation.getId()
                    );
                    // 재처리 성공 시 멱등성 저장
                    saveIdempotency(payment.getId(), PaymentOperation.CONFIRM, idempotencyKey, response);
                    log.info("[Payment] 좌석 SOLD 재처리 성공, 멱등성 저장 - paymentId: {}, reservationId: {}", 
                            payment.getId(), reservation.getId());
                } catch (Exception e) {
                    // 재처리 실패 시 Outbox 이벤트 저장
                    log.error("[Payment] 좌석 SOLD 재처리 실패, Outbox 이벤트 저장 - paymentId: {}, reservationId: {}", 
                            payment.getId(), reservation.getId(), e);
                    try {
                        outboxEventService.saveOutboxEvent(
                                payment.getId(),
                                PaymentEventType.SEAT_SOLD_RETRY,
                                createSeatSoldRetryPayload(payment, reservation)
                        );
                    } catch (Exception outboxException) {
                        log.error("[Payment] 좌석 SOLD 재처리 이벤트 저장 실패 - paymentId: {}", payment.getId(), outboxException);
                    }
                    // 멱등성 저장하지 않음 (재시도 가능하도록)
                }
            }
            
            return response;
        }

        // 멱등성 검증: TTL 체크 포함 조회
        LocalDateTime ttlDate = LocalDateTime.now().minusDays(idempotencyTtlDays);
        Optional<PaymentIdempotency> existingIdempotency = paymentIdempotencyRepository
                .findByPaymentIdAndOperationAndIdempotencyKeyWithTtl(
                    payment.getId(), PaymentOperation.CONFIRM.name(), idempotencyKey, ttlDate);
        
        if (existingIdempotency.isPresent()) {
            log.info("[Payment] 멱등성: 기존 승인 결과 반환 - paymentId: {}, idempotencyKey: {}", 
                    payment.getId(), idempotencyKey);
            return parseConfirmResponse(existingIdempotency.get().getResultJson(), payment);
        }

        // 조건부 UPDATE: CREATED → PROCESSING (Payment에 저장된 금액 사용)
        int updated = paymentRepository.updateStatusToProcessing(payment.getId(), payment.getAmount());
        if (updated == 0) {
            Payment currentPayment = paymentRepository.findById(payment.getId())
                    .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
            log.warn("[Payment] 상태 전이 실패 - paymentId: {}, 현재 상태: {}", 
                    payment.getId(), currentPayment.getStatus());
            throw new ServiceException(ErrorCode.PAYMENT_CANNOT_CONFIRM);
        }

        // 게이트웨이 호출 (토스페이먼츠는 orderId를 String으로 요구하므로 변환, Payment에 저장된 금액 사용)
        PaymentGatewayConfirmResponse gatewayResponse;
        try {
            gatewayResponse = paymentGateway.confirm(
                request.paymentKey(),
                String.valueOf(request.reservationId()),
                payment.getAmount()
            );
        } catch (Exception e) {
            // 타임아웃 시 PROCESSING_TIMEOUT으로 전이
            if (e.getMessage() != null && (e.getMessage().contains("timeout") || 
                e.getMessage().contains("Timeout") || 
                e.getClass().getSimpleName().contains("Timeout"))) {
                paymentRepository.updateStatusToTimeout(payment.getId());
                log.warn("[Payment] 게이트웨이 타임아웃 - paymentId: {}", payment.getId());
                throw new ServiceException(ErrorCode.PAYMENT_GATEWAY_TIMEOUT);
            }
            // 게이트웨이 호출 실패 시 CREATED로 롤백 (재시도 가능)
            paymentRepository.updateStatusToCreated(payment.getId());
            log.error("[Payment] 게이트웨이 호출 실패 - paymentId: {}", payment.getId(), e);
            throw new ServiceException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }

        if (gatewayResponse.success()) {
            return handleGatewaySuccess(payment.getId(), gatewayResponse, idempotencyKey);
        } else {
            return handleGatewayFailure(payment.getId(), gatewayResponse);
        }
    }

    private PaymentConfirmResponse handleGatewaySuccess(
            Long paymentId, 
            PaymentGatewayConfirmResponse gatewayResponse, 
            String idempotencyKey
    ) {
        LocalDateTime approvedAt = LocalDateTime.now();
        
        try {
            // 조건부 UPDATE: PROCESSING → APPROVED
            int approved = paymentRepository.updateStatusToApproved(
                paymentId, gatewayResponse.paymentKey(), approvedAt);
            
            if (approved == 0) {
                // 보상 트랜잭션: DB 업데이트 실패 시 게이트웨이 취소
                log.error("[Payment] 승인 상태 전이 실패, 게이트웨이 취소 시도 - paymentId: {}", paymentId);
                try {
                    paymentGateway.cancel(gatewayResponse.paymentKey(), "DB 업데이트 실패");
                } catch (Exception cancelException) {
                    log.error("[Payment] 게이트웨이 취소 실패 - paymentId: {}, paymentKey: {}", 
                            paymentId, gatewayResponse.paymentKey(), cancelException);
                }
                throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR);
            }

            Payment approvedPayment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
            
            // 결제 승인 후 예매된 좌석들을 SOLD 상태로 변경
            boolean seatSoldSuccess = false;
            try {
                Reservation reservation = reservationRepository.findById(approvedPayment.getReservationId())
                        .orElseThrow(() -> new ServiceException(ErrorCode.RESERVATION_NOT_FOUND));
                
                // 좌석을 SOLD로 표시
                reservationService.markSeatsAsSold(
                        reservation.getPerformanceScheduleId(),
                        reservation.getId()
                );
                
                seatSoldSuccess = true;
                log.info("[Payment] 좌석 SOLD 처리 완료 - reservationId: {}, paymentId: {}", 
                        approvedPayment.getReservationId(), paymentId);
            } catch (Exception e) {
                // 좌석 상태 변경 실패 시 Outbox 이벤트 저장 (재처리용)
                log.error("[Payment] 좌석 SOLD 상태 변경 실패, 재처리 이벤트 저장 - reservationId: {}, paymentId: {}", 
                        approvedPayment.getReservationId(), paymentId, e);
                try {
                    Reservation reservation = reservationRepository.findById(approvedPayment.getReservationId())
                            .orElse(null);
                    if (reservation != null) {
                        outboxEventService.saveOutboxEvent(
                                paymentId, 
                                PaymentEventType.SEAT_SOLD_RETRY, 
                                createSeatSoldRetryPayload(approvedPayment, reservation)
                        );
                        log.info("[Payment] 좌석 SOLD 재처리 이벤트 저장 완료 - paymentId: {}, reservationId: {}", 
                                paymentId, approvedPayment.getReservationId());
                    }
                } catch (Exception outboxException) {
                    log.error("[Payment] 좌석 SOLD 재처리 이벤트 저장 실패 - paymentId: {}", paymentId, outboxException);
                }
            }
            
            PaymentConfirmResponse response = PaymentMapper.toConfirmResponse(approvedPayment);
            
            // 멱등성 저장은 좌석 SOLD 성공 후에만 수행 (재시도 가능하도록)
            if (seatSoldSuccess) {
                saveIdempotency(paymentId, PaymentOperation.CONFIRM, idempotencyKey, response);
            } else {
                // 좌석 SOLD 실패 시 멱등성 저장하지 않음 (재시도 가능하도록)
                log.warn("[Payment] 좌석 SOLD 실패로 인해 멱등성 저장 생략 - paymentId: {}, reservationId: {}", 
                        paymentId, approvedPayment.getReservationId());
            }
            
            // Outbox 이벤트 저장 (REQUIRES_NEW로 분리)
            try {
                outboxEventService.saveOutboxEvent(approvedPayment.getId(), PaymentEventType.APPROVED, 
                        createApprovedPayload(approvedPayment, approvedAt));
            } catch (Exception e) {
                log.error("[Payment] Outbox 이벤트 저장 실패 (Payment 상태는 유지) - paymentId: {}", paymentId, e);
            }
            
            log.info("[Payment] 결제 승인 완료 - paymentId: {}, paymentKey: {}", 
                    approvedPayment.getId(), gatewayResponse.paymentKey());
            
            return response;
        } catch (Exception e) {
            // 보상 트랜잭션: 예외 발생 시 게이트웨이 취소
            log.error("[Payment] 승인 처리 중 예외 발생, 게이트웨이 취소 시도 - paymentId: {}", paymentId, e);
            try {
                paymentGateway.cancel(gatewayResponse.paymentKey(), "시스템 에러");
            } catch (Exception cancelException) {
                log.error("[Payment] 게이트웨이 취소 실패 - paymentId: {}", paymentId, cancelException);
            }
            throw e;
        }
    }

    private PaymentConfirmResponse handleGatewayFailure(
            Long paymentId, 
            PaymentGatewayConfirmResponse gatewayResponse
    ) {
        // 조건부 UPDATE: PROCESSING → FAILED
        int failed = paymentRepository.updateStatusToFailed(
                paymentId, gatewayResponse.failCode(), gatewayResponse.failMessage());
        if (failed == 0) {
            Payment currentPayment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
            log.warn("[Payment] 실패 상태 전이 실패 - paymentId: {}, 현재 상태: {}", 
                    paymentId, currentPayment.getStatus());
            throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        Payment failedPayment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
        
        // Outbox 이벤트 저장
        try {
            outboxEventService.saveOutboxEvent(failedPayment.getId(), PaymentEventType.FAILED, 
                    createFailedPayload(failedPayment, gatewayResponse));
        } catch (Exception e) {
            log.error("[Payment] Outbox 이벤트 저장 실패 - paymentId: {}", paymentId, e);
        }
        
        log.warn("[Payment] 결제 승인 실패 - paymentId: {}, failCode: {}, failMessage: {}",
                failedPayment.getId(), gatewayResponse.failCode(), gatewayResponse.failMessage());

        throw new ServiceException(ErrorCode.PAYMENT_GATEWAY_ERROR);
    }

    private PaymentConfirmResponse parseConfirmResponse(String resultJson, Payment payment) {
        // 멱등성 결과 파싱: resultJson 우선 사용, 실패 시 Payment 엔티티에서 생성
        try {
            if (resultJson != null && !resultJson.isEmpty()) {
                return objectMapper.readValue(resultJson, PaymentConfirmResponse.class);
            }
        } catch (Exception e) {
            log.warn("[Payment] Idempotency 결과 파싱 실패, Payment 엔티티에서 생성 - paymentId: {}", payment.getId(), e);
        }
        return PaymentMapper.toConfirmResponse(payment);
    }

    @Transactional
    public PaymentCancelResponse cancel(String paymentKey, PaymentCancelRequest request, Long userId, String idempotencyKey) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getUserId().equals(userId)) {
            log.warn("[Payment] 사용자 불일치 - paymentId: {}, 저장된 userId: {}, 요청 userId: {}",
                    payment.getId(), payment.getUserId(), userId);
            throw new ServiceException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        // 멱등성 검증: TTL 체크 포함 조회
        LocalDateTime ttlDate = LocalDateTime.now().minusDays(idempotencyTtlDays);
        Optional<PaymentIdempotency> existingIdempotency = paymentIdempotencyRepository
                .findByPaymentIdAndOperationAndIdempotencyKeyWithTtl(
                    payment.getId(), PaymentOperation.CANCEL.name(), idempotencyKey, ttlDate);
        
        if (existingIdempotency.isPresent()) {
            log.info("[Payment] 멱등성: 기존 취소 결과 반환 - paymentId: {}, idempotencyKey: {}", 
                    payment.getId(), idempotencyKey);
            return parseCancelResponse(existingIdempotency.get().getResultJson(), payment);
        }

        if (payment.getStatus() == PaymentStatus.CANCELED) {
            // 이미 취소됨: 멱등성 저장 후 반환
            PaymentCancelResponse response = PaymentMapper.toCancelResponse(payment);
            saveIdempotency(payment.getId(), PaymentOperation.CANCEL, idempotencyKey, response);
            return response;
        }

        if (payment.getStatus() != PaymentStatus.APPROVED) {
            log.warn("[Payment] 취소 불가능한 상태 - paymentId: {}, status: {}", payment.getId(), payment.getStatus());
            throw new ServiceException(ErrorCode.PAYMENT_CANNOT_CANCEL);
        }

        PaymentGatewayCancelResponse gatewayResponse = paymentGateway.cancel(paymentKey, request.reason());

        if (gatewayResponse.success()) {
            LocalDateTime canceledAt = LocalDateTime.now();
            int canceled = paymentRepository.updateStatusToCanceled(payment.getId(), canceledAt);
            if (canceled == 0) {
                Payment currentPayment = paymentRepository.findById(payment.getId())
                        .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
                log.warn("[Payment] 취소 상태 전이 실패 - paymentId: {}, 현재 상태: {}", 
                        payment.getId(), currentPayment.getStatus());
                throw new ServiceException(ErrorCode.PAYMENT_CANNOT_CANCEL);
            }

            Payment canceledPayment = paymentRepository.findById(payment.getId())
                    .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
            
            PaymentCancelResponse response = PaymentMapper.toCancelResponse(canceledPayment);
            
            // 멱등성 저장
            saveIdempotency(canceledPayment.getId(), PaymentOperation.CANCEL, idempotencyKey, response);
            
            // Outbox 이벤트 저장
            try {
                outboxEventService.saveOutboxEvent(canceledPayment.getId(), PaymentEventType.CANCELED, 
                        createCanceledPayload(canceledPayment, request.reason(), canceledAt));
            } catch (Exception e) {
                log.error("[Payment] Outbox 이벤트 저장 실패 - paymentId: {}", canceledPayment.getId(), e);
            }
            
            log.info("[Payment] 결제 취소 완료 - paymentId: {}, paymentKey: {}, reason: {}",
                    canceledPayment.getId(), paymentKey, request.reason());
            
            return response;
        } else {
            log.warn("[Payment] 결제 취소 실패 - paymentId: {}, failCode: {}, failMessage: {}",
                    payment.getId(), gatewayResponse.failCode(), gatewayResponse.failMessage());
            throw new ServiceException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
    }

    private PaymentCancelResponse parseCancelResponse(String resultJson, Payment payment) {
        // 멱등성 결과 파싱: resultJson 우선 사용, 실패 시 Payment 엔티티에서 생성
        try {
            if (resultJson != null && !resultJson.isEmpty()) {
                return objectMapper.readValue(resultJson, PaymentCancelResponse.class);
            }
        } catch (Exception e) {
            log.warn("[Payment] Idempotency 결과 파싱 실패, Payment 엔티티에서 생성 - paymentId: {}", payment.getId(), e);
        }
        return PaymentMapper.toCancelResponse(payment);
    }

    /**
     * 예매의 모든 좌석이 SOLD 상태인지 확인
     * @param reservationId 예매 ID
     * @return 모든 좌석이 SOLD 상태면 true, 하나라도 HOLD 상태면 false
     */
    private boolean checkSeatsSoldStatus(Long reservationId) {
        try {
            // ReservationSeat 조회
            List<ReservationSeat> reservationSeats = reservationSeatRepository.findByReservationId(reservationId);
            
            if (reservationSeats.isEmpty()) {
                log.warn("[Payment] 예매된 좌석이 없음 - reservationId: {}", reservationId);
                return false;
            }
            
            // PerformanceSeat ID 목록 추출
            List<Long> seatIds = reservationSeats.stream()
                    .map(ReservationSeat::getPerformanceSeatId)
                    .toList();
            
            // PerformanceSeat 조회
            List<PerformanceSeat> performanceSeats = performanceSeatRepository.findAllById(seatIds);
            
            // 모든 좌석이 SOLD 상태인지 확인
            boolean allSold = performanceSeats.stream()
                    .allMatch(seat -> seat.getStatus() == SeatStatus.SOLD);
            
            if (!allSold) {
                long holdCount = performanceSeats.stream()
                        .filter(seat -> seat.getStatus() == SeatStatus.HOLD)
                        .count();
                log.warn("[Payment] 좌석 상태 확인 - reservationId: {}, 전체: {}, HOLD: {}, SOLD: {}", 
                        reservationId, performanceSeats.size(), holdCount, 
                        performanceSeats.size() - holdCount);
            }
            
            return allSold;
        } catch (Exception e) {
            log.error("[Payment] 좌석 상태 확인 실패 - reservationId: {}", reservationId, e);
            return false;
        }
    }

    // 멱등성 저장
    private void saveIdempotency(Long paymentId, PaymentOperation operation, String idempotencyKey, Object result) {
        try {
            String resultJson = objectMapper.writeValueAsString(result);
            PaymentIdempotency idempotency = PaymentIdempotency.builder()
                    .paymentId(paymentId)
                    .operation(operation.name())
                    .idempotencyKey(idempotencyKey)
                    .resultJson(resultJson)
                    .build();
            paymentIdempotencyRepository.save(idempotency);
        } catch (Exception e) {
            log.error("[Payment] Idempotency 저장 실패 - paymentId: {}, operation: {}", 
                    paymentId, operation, e);
            throw new RuntimeException("Idempotency 저장 실패", e);
        }
    }

    private Map<String, Object> createBasePayload(Payment payment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", payment.getId());
        payload.put("reservationId", payment.getReservationId());
        payload.put("userId", payment.getUserId());
        payload.put("amount", payment.getAmount());
        return payload;
    }

    private Map<String, Object> createApprovedPayload(Payment payment, LocalDateTime approvedAt) {
        Map<String, Object> payload = createBasePayload(payment);
        payload.put("paymentKey", payment.getPaymentKey());
        payload.put("approvedAt", approvedAt.toString());
        return payload;
    }

    private Map<String, Object> createFailedPayload(Payment payment, PaymentGatewayConfirmResponse gatewayResponse) {
        Map<String, Object> payload = createBasePayload(payment);
        payload.put("failCode", gatewayResponse.failCode());
        payload.put("failMessage", gatewayResponse.failMessage());
        return payload;
    }

    private Map<String, Object> createCanceledPayload(Payment payment, String reason, LocalDateTime canceledAt) {
        Map<String, Object> payload = createBasePayload(payment);
        payload.put("paymentKey", payment.getPaymentKey());
        payload.put("cancelReason", reason);
        payload.put("canceledAt", canceledAt.toString());
        return payload;
    }

    private Map<String, Object> createSeatSoldRetryPayload(Payment payment, Reservation reservation) {
        Map<String, Object> payload = createBasePayload(payment);
        payload.put("paymentKey", payment.getPaymentKey());
        payload.put("scheduleId", reservation.getPerformanceScheduleId());
        payload.put("reservationId", reservation.getId());
        return payload;
    }
}
