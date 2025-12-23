package back.kalender.domain.payment.dto.response;

// 결제 게이트웨이 승인 응답 DTO (Service 레이어 전용)
public record PaymentGatewayConfirmResponse(
        boolean success,
        String paymentKey,
        String failCode,
        String failMessage
) {
    public static PaymentGatewayConfirmResponse ofSuccess(String paymentKey) {
        return new PaymentGatewayConfirmResponse(true, paymentKey, null, null);
    }

    public static PaymentGatewayConfirmResponse ofFailure(String failCode, String failMessage) {
        return new PaymentGatewayConfirmResponse(false, null, failCode, failMessage);
    }
}
