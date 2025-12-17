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

    private Integer floor; 
    private String block;
    private Integer rowNumber;
    private Integer seatNumber;

    private Integer x;
    private Integer y;

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
