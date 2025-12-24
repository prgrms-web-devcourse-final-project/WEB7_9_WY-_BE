package back.kalender.domain.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

// 토스페이먼츠 결제 취소 응답 DTO
public record TossPaymentCancelResponse(
        @JsonProperty("paymentKey")
        String paymentKey,
        
        @JsonProperty("orderId")
        String orderId,
        
        @JsonProperty("status")
        String status,
        
        @JsonProperty("cancelReason")
        String cancelReason,
        
        @JsonProperty("canceledAt")
        String canceledAt,
        
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

