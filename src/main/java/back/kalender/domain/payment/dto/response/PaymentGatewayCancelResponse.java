package back.kalender.domain.payment.dto.response;

// 결제 게이트웨이 취소 응답 DTO (Service 레이어 전용)
public record PaymentGatewayCancelResponse(
        boolean success,
        String failCode,
        String failMessage
) {
    public static PaymentGatewayCancelResponse ofSuccess() {
        return new PaymentGatewayCancelResponse(true, null, null);
    }

    public static PaymentGatewayCancelResponse ofFailure(String failCode, String failMessage) {
        return new PaymentGatewayCancelResponse(false, failCode, failMessage);
    }
}
