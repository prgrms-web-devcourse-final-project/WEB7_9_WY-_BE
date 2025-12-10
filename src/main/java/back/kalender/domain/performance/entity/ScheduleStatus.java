package back.kalender.domain.performance.entity;

public enum ScheduleStatus {
    AVAILABLE("예매 가능"),
    SOLD_OUT("매진"),
    CLOSED("예매 마감"),
    CANCELLED("취소됨");

    private final String description;

    ScheduleStatus(String description) {
        this.description = description;
    }
}
