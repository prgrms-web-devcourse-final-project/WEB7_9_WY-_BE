package back.kalender.domain.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 결제 상태
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    CREATED("생성됨"),
    PROCESSING("처리 중"),
    PROCESSING_TIMEOUT("처리 중 타임아웃"),
    APPROVED("승인됨"),
    FAILED("실패"),
    CANCELED("취소됨");

    private final String description;
}
