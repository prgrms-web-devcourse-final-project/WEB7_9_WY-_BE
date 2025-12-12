package back.kalender.domain.performance.hallSeat.entity;

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

    @Column(name = "hall_id")
    private Long hallId;

    private Integer floor;   // 1층, 2층, 3층

    private String block;    // A, B, C, 1층A 등

    @Column(name = "row_number")
    private Integer row;     // 행 번호
    private Integer number;  // 좌석 번호

    private Integer x;       // 좌표
    private Integer y;

    @Enumerated(EnumType.STRING)
    private SeatType seatType;   // NORMAL, WHEELCHAIR, LIMITED_VIEW, STANDING

    public enum SeatType {
        NORMAL,
        WHEELCHAIR,
        LIMITED_VIEW,
        STANDING
    }

}


