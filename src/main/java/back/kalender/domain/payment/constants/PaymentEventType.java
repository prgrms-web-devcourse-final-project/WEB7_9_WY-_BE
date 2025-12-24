package back.kalender.domain.payment.constants;

// 결제 이벤트 타입 상수
public class PaymentEventType {
    public static final String APPROVED = "PAYMENT_APPROVED";
    public static final String FAILED = "PAYMENT_FAILED";
    public static final String CANCELED = "PAYMENT_CANCELED";
    public static final String SEAT_SOLD_RETRY = "SEAT_SOLD_RETRY";
    public static final String SEAT_SOLD_FAILED = "SEAT_SOLD_FAILED";
    
    private PaymentEventType() {
        // 인스턴스화 방지
    }
}
