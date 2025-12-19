package back.kalender.domain.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 결제 게이트웨이 취소 응답 DTO (Service 레이어 전용)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayCancelResponse {
    private boolean success;
    private String failCode;
    private String failMessage;

    public static PaymentGatewayCancelResponse success() {
        return PaymentGatewayCancelResponse.builder()
                .success(true)
                .build();
    }

    public static PaymentGatewayCancelResponse failure(String failCode, String failMessage) {
        return PaymentGatewayCancelResponse.builder()
                .success(false)
                .failCode(failCode)
                .failMessage(failMessage)
                .build();
    }
}
