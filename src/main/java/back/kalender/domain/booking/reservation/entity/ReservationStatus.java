package back.kalender.domain.booking.reservation.entity;

import lombok.Getter;

@Getter
public enum ReservationStatus {
    HOLD("임시 예매"),
    PAID("결제 완료"),
    EXPIRED("만료됨"),
    CANCELLED("취소됨");

    private final String description;

    ReservationStatus(String description) {
        this.description = description;
    }

    public boolean isHold() {
        return this == HOLD;
    }

    public boolean isPaid() {
        return this == PAID;
    }

    public boolean isExpired() {
        return this == EXPIRED;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }

    public boolean canConfirm() {
        return this == HOLD;
    }

    public boolean canCancel() {
        return this == HOLD;
    }
}
