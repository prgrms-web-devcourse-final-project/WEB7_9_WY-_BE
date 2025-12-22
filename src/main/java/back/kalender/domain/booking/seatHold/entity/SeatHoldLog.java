package back.kalender.domain.booking.seatHold.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "seat_hold_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatHoldLog extends BaseEntity {
    @Column(name = "performance_seat_id", nullable = false)
    private Long performanceSeatId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "hold_started_at", nullable = false)
    private LocalDateTime holdStartedAt;

    @Column(name = "hold_expired_at", nullable = false)
    private LocalDateTime holdExpiredAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "is_expired", nullable = false)
    private Boolean isExpired;

    @Builder
    public SeatHoldLog(
            Long performanceSeatId,
            Long userId,
            LocalDateTime holdStartedAt,
            LocalDateTime holdExpiredAt
    ) {
        this.performanceSeatId = performanceSeatId;
        this.userId = userId;
        this.holdStartedAt = holdStartedAt;
        this.holdExpiredAt = holdExpiredAt;
        this.isExpired = false;
    }

    // HOLD 해제 기록
    public void markReleased() {
        this.releasedAt = LocalDateTime.now();
    }

    // 만료 처리
    public void markExpired() {
        this.isExpired = true;
    }
}
