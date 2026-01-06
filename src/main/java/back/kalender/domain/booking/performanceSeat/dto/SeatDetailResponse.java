package back.kalender.domain.booking.performanceSeat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SeatDetailResponse {

    private Long seatId;
    private Integer rowNumber;
    private Integer seatNumber;
    private Long priceGradeId;
}
