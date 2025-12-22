package back.kalender.domain.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Outbox 이벤트 상태
@Getter
@RequiredArgsConstructor
public enum OutboxStatus {
    PENDING("대기 중"),
    PROCESSING("처리 중"),
    SENT("전송 완료"),
    FAILED("실패"),
    ABANDONED("포기됨");

    private final String description;
}
