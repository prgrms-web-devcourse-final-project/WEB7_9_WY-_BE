package back.kalender.domain.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 결제 게이트웨이 승인 응답 DTO (Service 레이어 전용)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayConfirmResponse {
    private boolean success;
    private String paymentKey;
    private String failCode;
    private String failMessage;

    public static PaymentGatewayConfirmResponse success(String paymentKey) {
        return PaymentGatewayConfirmResponse.builder()
                .success(true)
                .paymentKey(paymentKey)
                .build();
    }

    public static PaymentGatewayConfirmResponse failure(String failCode, String failMessage) {
        return PaymentGatewayConfirmResponse.builder()
                .success(false)
                .failCode(failCode)
                .failMessage(failMessage)
                .build();
    }
}
