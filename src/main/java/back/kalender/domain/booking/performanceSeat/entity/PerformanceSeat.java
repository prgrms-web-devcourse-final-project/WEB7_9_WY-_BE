package back.kalender.domain.booking.performanceSeat.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "performance_seats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PerformanceSeat extends BaseEntity {

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "hall_seat_id", nullable = false)
    private Long hallSeatId;

    @Column(name = "price_grade_id", nullable = false)
    private Long priceGradeId;

    private Integer floor; 
    private String block;
    private Integer rowNumber;
    private Integer seatNumber;

    private Integer x;
    private Integer y;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatStatus status;

    @Column(name = "hold_user_id")
    private Long holdUserId;

    @Column(name = "hold_expired_at")
    private LocalDateTime holdExpiredAt;

    public static PerformanceSeat create(
            Long scheduleId,
            Long hallSeatId,
            Long priceGradeId,
            Integer floor,
            String block,
            Integer rowNumber,
            Integer seatNumber,
            Integer x,
            Integer y
    ) {
        PerformanceSeat seat = new PerformanceSeat();
        seat.scheduleId = scheduleId;
        seat.hallSeatId = hallSeatId;
        seat.priceGradeId = priceGradeId;
        seat.floor = floor;
        seat.block = block;
        seat.rowNumber = rowNumber;
        seat.seatNumber = seatNumber;
        seat.x = x;
        seat.y = y;
        seat.status = SeatStatus.AVAILABLE; // 기본값
        return seat;
    }

    // 좌석 상태 변경
    public void updateStatus(SeatStatus status) {
        this.status = status;
    }

    // HOLD 정보 설정
    public void updateHoldInfo(Long userId, LocalDateTime expiredAt) {
        this.holdUserId = userId;
        this.holdExpiredAt = expiredAt;
    }

    // HOLD 정보 초기화
    public void clearHoldInfo() {
        this.holdUserId = null;
        this.holdExpiredAt = null;
    }

    // HOLD 만료 여부 확인
    public boolean isHoldExpired(LocalDateTime now) {
        if(this.status != SeatStatus.HOLD){
            return false;
        }
        return holdExpiredAt != null && now.isAfter(holdExpiredAt);
    }

    // 좌석 선점 가능 여부
    public boolean canBeHeld(){
        // AVAILABLE 이거나, HOLD 상태이지만 만료된 경우
        return this.status == SeatStatus.AVAILABLE || (this.status == SeatStatus.HOLD && isHoldExpired(LocalDateTime.now()));
    }

}
