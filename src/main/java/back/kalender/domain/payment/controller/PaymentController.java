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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController implements PaymentControllerSpec {

    private final PaymentService paymentService;

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
        Long userId = SecurityUtil.getCurrentUserIdOrThrow();
        PaymentConfirmResponse response = paymentService.confirm(request, userId, idempotencyKey);
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
        PaymentResponse response = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(response);
    }
}
