package back.kalender.domain.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Outbox 이벤트 상태
@Getter
@RequiredArgsConstructor
public enum OutboxStatus {
    PENDING("대기 중"),
    SENT("전송 완료"),
    FAILED("실패");

    private final String description;
}

