package back.kalender.domain.booking.performanceSeat.dto;

import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PerformanceSeatResponse {

    private Long performanceSeatId;

    private Integer floor;
    private String block;
    private Integer rowNumber;
    private Integer seatNumber;

    private Integer x;
    private Integer y;

    private Long priceGradeId;

    public static PerformanceSeatResponse from(PerformanceSeat seat) {
        return new PerformanceSeatResponse(
                seat.getId(),
                seat.getFloor(),
                seat.getBlock(),
                seat.getRowNumber(),
                seat.getSeatNumber(),
                seat.getX(),
                seat.getY(),
                seat.getPriceGradeId()
        );
    }
}
