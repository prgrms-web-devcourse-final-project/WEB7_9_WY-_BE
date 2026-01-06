package back.kalender.domain.payment.controller;

import back.kalender.domain.payment.dto.request.PaymentCancelRequest;
import back.kalender.domain.payment.dto.request.PaymentConfirmRequest;
import back.kalender.domain.payment.dto.request.PaymentCreateRequest;
import back.kalender.domain.payment.dto.response.PaymentCancelResponse;
import back.kalender.domain.payment.dto.response.PaymentConfirmResponse;
import back.kalender.domain.payment.dto.response.PaymentCreateResponse;
import back.kalender.domain.payment.dto.response.PaymentResponse;
import back.kalender.domain.payment.service.PaymentService;
import back.kalender.global.security.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController implements PaymentControllerSpec {

    private final PaymentService paymentService;

    @Value("${custom.payment.toss.clientKey:}")
    private String clientKey;

    @PostMapping
    public ResponseEntity<PaymentCreateResponse> createPayment(
            @Valid @RequestBody PaymentCreateRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        Long userId = SecurityUtil.getCurrentUserIdOrThrow();
        PaymentCreateResponse response = paymentService.create(request, idempotencyKey, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(
            @Valid @RequestBody PaymentConfirmRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        log.info("[PaymentController] 결제 확인 API 진입 - paymentKey: {}, reservationId: {}, orderId: {}, idempotencyKey: {}",
                request.paymentKey(), request.reservationId(), request.orderId(), idempotencyKey);
        Long userId = SecurityUtil.getCurrentUserIdOrThrow();
        log.info("[PaymentController] 결제 확인 API - userId 조회 성공: {}", userId);
        PaymentConfirmResponse response = paymentService.confirm(request, userId, idempotencyKey);
        log.info("[PaymentController] 결제 확인 API - 처리 완료, paymentKey: {}, reservationId: {}",
                request.paymentKey(), request.reservationId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentKey}/cancel")
    public ResponseEntity<PaymentCancelResponse> cancelPayment(
            @PathVariable String paymentKey,
            @Valid @RequestBody PaymentCancelRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        Long userId = SecurityUtil.getCurrentUserIdOrThrow();
        PaymentCancelResponse response = paymentService.cancel(paymentKey, request, userId, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable Long paymentId
    ) {
        Long userId = SecurityUtil.getCurrentUserIdOrThrow();
        PaymentResponse response = paymentService.getPayment(paymentId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/client-key")
    public ResponseEntity<Map<String, String>> getClientKey() {
        Map<String, String> response = new HashMap<>();
        response.put("clientKey", clientKey);
        return ResponseEntity.ok(response);
    }
}
