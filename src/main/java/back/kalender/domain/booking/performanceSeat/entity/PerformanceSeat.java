package back.kalender.domain.booking.performanceSeat.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    // 정적 좌석 구조 복사
    private Integer floor; 
    private String block;
    private Integer rowNumber;
    private Integer seatNumber;

    private Integer x;
    private Integer y;

    /**
     * 생성 전용 정적 팩토리
     * (B가 상태값들 붙이기 전에는 이 구조만 사용)
     */
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
        return new PerformanceSeat(
                scheduleId,
                hallSeatId,
                priceGradeId,
                floor,
                block,
                rowNumber,
                seatNumber,
                x,
                y
        );
    }
}
