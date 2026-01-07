package back.kalender.domain.booking.reservation.entity;

import lombok.Getter;

import java.util.List;

@Getter
public enum ReservationStatus {
    PENDING("초기 상태"),
    HOLD("홀드 상태"),
    PAID("결제 완료"),
    EXPIRED("만료됨"),
    CANCELLED("취소됨"),
    ABANDONED("포기/중단됨");    // 브라우저 닫기, Active sweep 등

    private final String description;

    ReservationStatus(String description) {
        this.description = description;
    }

    public boolean isPending() {
        return this == PENDING;
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

    public boolean isAbandoned() {
        return this == ABANDONED;
    }

    public boolean canConfirm() {
        return this == HOLD;
    }

    public boolean canCancel() {
        return this == HOLD;
    }

    public static List<ReservationStatus> activeStatuses() {
        return List.of(PENDING, HOLD);
    }

    /**
     * 마이페이지 "예매 내역"에 표시할 상태들
     * - PAID: 결제 완료 (정상)
     * - CANCELLED: 사용자가 명시적으로 취소한 건만
     */
    public static List<ReservationStatus> visibleStatuses() {
        return List.of(PAID, CANCELLED);
    }
}
