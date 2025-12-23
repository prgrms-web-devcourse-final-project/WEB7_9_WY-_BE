package back.kalender.domain.payment.service;

import back.kalender.domain.payment.dto.response.PaymentGatewayConfirmResponse;
import back.kalender.domain.payment.dto.response.PaymentGatewayCancelResponse;

// 결제 게이트웨이 인터페이스 (Gateway 패턴: 외부 결제사 API를 추상화)
public interface PaymentGateway {

    // 결제 승인 요청 (토스페이먼츠 API 호출)
    PaymentGatewayConfirmResponse confirm(String paymentKey, String orderId, Integer amount);

    // 결제 취소 요청 (토스페이먼츠 API 호출)
    PaymentGatewayCancelResponse cancel(String paymentKey, String cancelReason);
}
