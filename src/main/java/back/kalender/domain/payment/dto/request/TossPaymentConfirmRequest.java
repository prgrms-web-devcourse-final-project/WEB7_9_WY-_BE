package back.kalender.domain.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

// 토스페이먼츠 결제 승인 요청 DTO
public record TossPaymentConfirmRequest(
        @JsonProperty("paymentKey")
        String paymentKey,
        
        @JsonProperty("orderId")
        String orderId,
        
        @JsonProperty("amount")
        Integer amount
) {
}

