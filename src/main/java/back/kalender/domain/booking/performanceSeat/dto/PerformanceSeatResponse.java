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
    private String subBlock;

    private Integer rowNumber;
    private Integer seatNumber;


    private Long priceGradeId;

    public static PerformanceSeatResponse from(PerformanceSeat seat) {
        return new PerformanceSeatResponse(
                seat.getId(),
                seat.getFloor(),
                seat.getBlock(),
                seat.getSubBlock(),
                seat.getRowNumber(),
                seat.getSeatNumber(),
                seat.getPriceGradeId()
        );
    }
}
