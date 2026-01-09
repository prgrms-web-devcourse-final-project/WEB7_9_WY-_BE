package back.kalender.domain.booking.performanceSeat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SeatDetailResponse {
    private final Long seatId;
    private final int rowNumber;
    private final int seatNumber;
    private final Long priceGradeId;

    public static SeatDetailResponse of(Long seatId, int rowNumber, int seatNumber, Long priceGradeId) {
        return new SeatDetailResponse(seatId, rowNumber, seatNumber, priceGradeId);
    }
}
