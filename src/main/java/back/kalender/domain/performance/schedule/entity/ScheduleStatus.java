package back.kalender.domain.performance.schedule.entity;

public enum ScheduleStatus {
    READY("예매 준비중"),          // 판매 대기 (salesStartTime 이전)
    AVAILABLE("예매 가능"),
    SOLD_OUT("매진"),
    CLOSED("예매 마감"),
    CANCELLED("취소됨");

    private final String description;

    ScheduleStatus(String description) {
        this.description = description;
    }
}
