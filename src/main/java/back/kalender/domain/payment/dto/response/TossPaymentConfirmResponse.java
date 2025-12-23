package back.kalender.domain.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

// 토스페이먼츠 결제 승인 응답 DTO
public record TossPaymentConfirmResponse(
        @JsonProperty("paymentKey")
        String paymentKey,
        
        @JsonProperty("orderId")
        String orderId,
        
        @JsonProperty("status")
        String status,
        
        @JsonProperty("totalAmount")
        Integer totalAmount,
        
        @JsonProperty("method")
        String method,
        
        @JsonProperty("approvedAt")
        String approvedAt,
        
        @JsonProperty("failReason")
        FailReason failReason
) {
    public record FailReason(
            @JsonProperty("code")
            String code,
            
            @JsonProperty("message")
            String message
    ) {
    }
}

