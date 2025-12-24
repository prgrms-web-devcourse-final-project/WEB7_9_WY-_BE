package back.kalender.domain.performance.hallSeat.entity;

import back.kalender.domain.performance.performanceHall.entity.PerformanceHall;
import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hall_seats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class HallSeat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_hall_id", nullable = false)
    private PerformanceHall performanceHall;

    private Integer floor;   // 1층, 2층 등
    private String block;
    @Column(name = "sub_block")
    private String subBlock;
    @Column(name = "row_number")
    private Integer rowNumber;
    private Integer seatNumber;

    private Integer x;
    private Integer y;

    @Enumerated(EnumType.STRING)
    private SeatType seatType;

    public enum SeatType {
        NORMAL,
        WHEELCHAIR,
        LIMITED_VIEW,
        STANDING
    }
}
