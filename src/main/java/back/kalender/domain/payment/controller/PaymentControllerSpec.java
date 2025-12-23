package back.kalender.domain.payment.controller;

import back.kalender.domain.payment.dto.request.PaymentCancelRequest;
import back.kalender.domain.payment.dto.request.PaymentConfirmRequest;
import back.kalender.domain.payment.dto.request.PaymentCreateRequest;
import back.kalender.domain.payment.dto.response.PaymentCancelResponse;
import back.kalender.domain.payment.dto.response.PaymentConfirmResponse;
import back.kalender.domain.payment.dto.response.PaymentCreateResponse;
import back.kalender.domain.payment.dto.response.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

// 결제 API 스펙 (Swagger 전용)
@Tag(name = "Payment", description = "결제 API")
public interface PaymentControllerSpec {

    @Operation(summary = "결제 생성", description = "새로운 결제를 생성합니다. Idempotency-Key 헤더가 필수입니다.")
    ResponseEntity<PaymentCreateResponse> createPayment(
            @Valid @RequestBody PaymentCreateRequest request,
            @RequestHeader("Idempotency-Key")
            @Parameter(description = "멱등성 키 (UUID 권장)", required = true) String idempotencyKey
    );

    @Operation(summary = "결제 승인", description = "결제를 최종 승인합니다. Idempotency-Key 헤더가 필수입니다.")
    ResponseEntity<PaymentConfirmResponse> confirmPayment(
            @Valid @RequestBody PaymentConfirmRequest request,
            @RequestHeader("Idempotency-Key")
            @Parameter(description = "멱등성 키 (UUID 권장)", required = true) String idempotencyKey
    );

    @Operation(summary = "결제 취소", description = "승인된 결제를 취소합니다. Idempotency-Key 헤더가 필수입니다.")
    ResponseEntity<PaymentCancelResponse> cancelPayment(
            @PathVariable
            @Parameter(description = "토스페이먼츠 결제 키", required = true) String paymentKey,
            @Valid @RequestBody PaymentCancelRequest request,
            @RequestHeader("Idempotency-Key")
            @Parameter(description = "멱등성 키 (UUID 권장)", required = true) String idempotencyKey
    );

    @Operation(summary = "결제 조회", description = "결제 상세 정보를 조회합니다.")
    ResponseEntity<PaymentResponse> getPayment(
            @PathVariable
            @Parameter(description = "결제 ID", required = true) Long paymentId
    );
}

