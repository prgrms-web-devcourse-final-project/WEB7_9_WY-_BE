package back.kalender.domain.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 결제 제공자
@Getter
@RequiredArgsConstructor
public enum PaymentProvider {
    TOSS("토스페이먼츠");

    private final String name;
}

