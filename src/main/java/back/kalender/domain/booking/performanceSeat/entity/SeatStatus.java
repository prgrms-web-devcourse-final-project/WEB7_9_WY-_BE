package back.kalender.domain.booking.performanceSeat.entity;

import lombok.Getter;

@Getter
public enum SeatStatus {
    AVAILABLE("예매 가능"),
    HOLD("임시 선점"),
    SOLD("판매 완료");

    private final String description;

    SeatStatus(String description) {
        this.description = description;
    }

    public boolean isAvailable() {
        return this == AVAILABLE;
    }

    public boolean isHold() {
        return this == HOLD;
    }

    public boolean isSold() {
        return this == SOLD;
    }
}
