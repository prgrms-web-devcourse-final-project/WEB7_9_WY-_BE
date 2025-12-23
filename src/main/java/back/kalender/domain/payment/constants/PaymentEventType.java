package back.kalender.domain.payment.constants;

// 결제 이벤트 타입 상수
public class PaymentEventType {
    public static final String APPROVED = "PAYMENT_APPROVED";
    public static final String FAILED = "PAYMENT_FAILED";
    public static final String CANCELED = "PAYMENT_CANCELED";
    
    private PaymentEventType() {
        // 인스턴스화 방지
    }
}
