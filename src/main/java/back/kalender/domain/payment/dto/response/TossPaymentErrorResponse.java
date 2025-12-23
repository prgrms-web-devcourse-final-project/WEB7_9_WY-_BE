package back.kalender.domain.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

// 토스페이먼츠 에러 응답 DTO
public record TossPaymentErrorResponse(
        @JsonProperty("code")
        String code,
        
        @JsonProperty("message")
        String message
) {
}

