package back.kalender.domain.payment.service;

import back.kalender.domain.payment.dto.response.PaymentGatewayCancelResponse;
import back.kalender.domain.payment.dto.response.PaymentGatewayConfirmResponse;
import org.springframework.stereotype.Component;

// 토스페이먼츠 결제 게이트웨이 구현체 (현재 stub)
@Component
public class TossPaymentGateway implements PaymentGateway {

    @Override
    public PaymentGatewayConfirmResponse confirm(String paymentKey, String orderId, Integer amount) {
        // TODO: 실제 토스페이먼츠 API 호출 구현
        throw new UnsupportedOperationException("TODO: Implement Toss Payment API call");
    }

    @Override
    public PaymentGatewayCancelResponse cancel(String paymentKey, String cancelReason) {
        // TODO: 실제 토스페이먼츠 취소 API 호출 구현
        throw new UnsupportedOperationException("TODO: Implement Toss Payment Cancel API call");
    }
}
