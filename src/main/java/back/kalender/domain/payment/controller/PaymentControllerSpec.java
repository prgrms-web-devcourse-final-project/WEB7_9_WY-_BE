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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

// 결제 API 스펙 (Swagger 전용)
@Tag(name = "Payment", description = "결제 API - 토스페이먼츠 연동")
public interface PaymentControllerSpec {

    @Operation(
            summary = "결제 생성",
            description = """
                    새로운 결제를 생성합니다.
                    
                    **주의사항:**
                    - `Idempotency-Key` 헤더가 필수입니다 (UUID 권장)
                    - 예매(Reservation)가 HOLD 상태여야 합니다
                    - 실제로 홀드된 좌석이 있어야 합니다
                    - 예매가 만료되지 않아야 합니다
                    - 결제 금액은 예매의 totalAmount를 사용합니다 (요청의 amount는 무시됨)
                    
                    **멱등성:**
                    - 동일한 `Idempotency-Key`로 재요청 시 기존 결제를 반환합니다
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "결제 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaymentCreateResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "paymentId": 1,
                                      "reservationId": 123,
                                      "amount": 50000,
                                      "status": "CREATED"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Idempotency-Key 누락",
                                            value = """
                                                    {
                                                      "error": {
                                                        "code": "9008",
                                                        "status": "400",
                                                        "message": "Idempotency-Key 헤더가 필요합니다."
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "예매가 HOLD 상태가 아님",
                                            value = """
                                                    {
                                                      "error": {
                                                        "code": "6010",
                                                        "status": "400",
                                                        "message": "예매가 홀드 상태가 아닙니다."
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "홀드된 좌석 없음",
                                            value = """
                                                    {
                                                      "error": {
                                                        "code": "6011",
                                                        "status": "400",
                                                        "message": "홀드된 좌석이 없습니다."
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "결제 금액 불일치",
                                            value = """
                                                    {
                                                      "error": {
                                                        "code": "9002",
                                                        "status": "400",
                                                        "message": "결제 금액이 일치하지 않습니다."
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "002",
                                        "status": "401",
                                        "message": "로그인이 필요합니다."
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "예매를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "6001",
                                        "status": "404",
                                        "message": "예매를 찾을 수 없습니다."
                                      }
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<PaymentCreateResponse> createPayment(
            @Valid @RequestBody PaymentCreateRequest request,
            @RequestHeader("Idempotency-Key")
            @Parameter(description = "멱등성 키 (UUID 권장)", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            String idempotencyKey
    );

    @Operation(
            summary = "결제 승인",
            description = """
                    결제를 최종 승인합니다. 토스페이먼츠 게이트웨이를 통해 실제 결제를 처리합니다.
                    
                    **주의사항:**
                    - `Idempotency-Key` 헤더가 필수입니다 (UUID 권장)
                    - 결제 상태가 `CREATED`여야 합니다
                    - 결제 승인 성공 시 예매된 좌석이 자동으로 SOLD 상태로 변경됩니다
                    - 동일한 `Idempotency-Key`로 재요청 시 기존 승인 결과를 반환합니다
                    
                    **결제 플로우:**
                    1. 결제 상태를 `PROCESSING`으로 변경
                    2. 토스페이먼츠 게이트웨이에 결제 승인 요청
                    3. 승인 성공 시:
                       - 결제 상태를 `APPROVED`로 변경
                       - 예매 좌석을 SOLD 상태로 변경
                       - Outbox 이벤트 발행
                    4. 승인 실패 시:
                       - 결제 상태를 `FAILED`로 변경
                       - 실패 정보 저장
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "결제 승인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaymentConfirmResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "paymentId": 1,
                                      "reservationId": 123,
                                      "status": "APPROVED",
                                      "approvedAt": "2025-01-01T12:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Idempotency-Key 누락",
                                            value = """
                                                    {
                                                      "error": {
                                                        "code": "9008",
                                                        "status": "400",
                                                        "message": "Idempotency-Key 헤더가 필요합니다."
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "승인 불가능한 상태",
                                            value = """
                                                    {
                                                      "error": {
                                                        "code": "9007",
                                                        "status": "400",
                                                        "message": "승인할 수 없는 결제 상태입니다."
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "주문 ID 불일치",
                                            value = """
                                                    {
                                                      "error": {
                                                        "code": "9003",
                                                        "status": "400",
                                                        "message": "주문 ID가 일치하지 않습니다."
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "002",
                                        "status": "401",
                                        "message": "로그인이 필요합니다."
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "결제를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "9001",
                                        "status": "404",
                                        "message": "결제를 찾을 수 없습니다."
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "결제 게이트웨이 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "9006",
                                        "status": "502",
                                        "message": "결제 게이트웨이 오류가 발생했습니다."
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "504",
                    description = "결제 게이트웨이 타임아웃",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "9010",
                                        "status": "504",
                                        "message": "결제 게이트웨이 타임아웃이 발생했습니다."
                                      }
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<PaymentConfirmResponse> confirmPayment(
            @Valid @RequestBody PaymentConfirmRequest request,
            @RequestHeader("Idempotency-Key")
            @Parameter(description = "멱등성 키 (UUID 권장)", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            String idempotencyKey
    );

    @Operation(
            summary = "결제 취소",
            description = """
                    승인된 결제를 취소합니다. 토스페이먼츠 게이트웨이를 통해 실제 결제 취소를 처리합니다.
                    
                    **주의사항:**
                    - `Idempotency-Key` 헤더가 필수입니다 (UUID 권장)
                    - 결제 상태가 `APPROVED`여야 합니다
                    - 동일한 `Idempotency-Key`로 재요청 시 기존 취소 결과를 반환합니다
                    
                    **취소 플로우:**
                    1. 결제 상태 검증 (APPROVED 여부 확인)
                    2. 토스페이먼츠 게이트웨이에 결제 취소 요청
                    3. 취소 성공 시:
                       - 결제 상태를 `CANCELED`로 변경
                       - 취소 사유 저장
                       - Outbox 이벤트 발행
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "결제 취소 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaymentCancelResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "paymentId": 1,
                                      "status": "CANCELED",
                                      "canceledAt": "2025-01-01T13:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Idempotency-Key 누락",
                                            value = """
                                                    {
                                                      "error": {
                                                        "code": "9008",
                                                        "status": "400",
                                                        "message": "Idempotency-Key 헤더가 필요합니다."
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "취소 불가능한 상태",
                                            value = """
                                                    {
                                                      "error": {
                                                        "code": "9004",
                                                        "status": "400",
                                                        "message": "취소할 수 없는 결제 상태입니다."
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "이미 취소됨",
                                            value = """
                                                    {
                                                      "error": {
                                                        "code": "9005",
                                                        "status": "400",
                                                        "message": "이미 취소된 결제입니다."
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "002",
                                        "status": "401",
                                        "message": "로그인이 필요합니다."
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "결제를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "9001",
                                        "status": "404",
                                        "message": "결제를 찾을 수 없습니다."
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "결제 게이트웨이 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "9006",
                                        "status": "502",
                                        "message": "결제 게이트웨이 오류가 발생했습니다."
                                      }
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<PaymentCancelResponse> cancelPayment(
            @PathVariable
            @Parameter(description = "토스페이먼츠 결제 키", required = true, example = "tgen_20250101_abc123")
            String paymentKey,
            @Valid @RequestBody PaymentCancelRequest request,
            @RequestHeader("Idempotency-Key")
            @Parameter(description = "멱등성 키 (UUID 권장)", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            String idempotencyKey
    );

    @Operation(
            summary = "결제 조회",
            description = """
                    결제 상세 정보를 조회합니다.
                    
                    **주의사항:**
                    - 본인이 생성한 결제만 조회 가능합니다
                    - 결제 상태, 금액, 승인 시간 등 모든 정보를 포함합니다
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "paymentId": 1,
                                      "reservationId": 123,
                                      "userId": 100,
                                      "provider": "TOSS",
                                      "paymentKey": "tgen_20250101_abc123",
                                      "amount": 50000,
                                      "currency": "KRW",
                                      "method": "카드",
                                      "status": "APPROVED",
                                      "failCode": null,
                                      "failMessage": null,
                                      "approvedAt": "2025-01-01T12:00:00",
                                      "canceledAt": null,
                                      "createdAt": "2025-01-01T11:00:00",
                                      "updatedAt": "2025-01-01T12:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "002",
                                        "status": "401",
                                        "message": "로그인이 필요합니다."
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "결제를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "9001",
                                        "status": "404",
                                        "message": "결제를 찾을 수 없습니다."
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": {
                                        "code": "005",
                                        "status": "403",
                                        "message": "권한이 없습니다."
                                      }
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<PaymentResponse> getPayment(
            @PathVariable
            @Parameter(description = "결제 ID", required = true, example = "1")
            Long paymentId
    );

    @Operation(
            summary = "클라이언트 키 조회",
            description = """
                    토스페이먼츠 클라이언트 키를 조회합니다.
                    
                    **주의사항:**
                    - 인증이 필요하지 않은 공개 엔드포인트입니다
                    - 프론트엔드에서 결제창 초기화 시 사용합니다
                    - 환경 변수 `TOSS_PAYMENT_CLIENT_KEY`에서 값을 가져옵니다
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "clientKey": "test_ck_xxxxx"
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<Map<String, String>> getClientKey();
}

