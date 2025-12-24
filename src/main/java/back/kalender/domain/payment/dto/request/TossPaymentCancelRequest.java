package back.kalender.domain.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

// 토스페이먼츠 결제 취소 요청 DTO
public record TossPaymentCancelRequest(
        @JsonProperty("cancelReason")
        String cancelReason
) {
}

