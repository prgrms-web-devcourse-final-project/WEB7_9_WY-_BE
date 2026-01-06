package back.kalender.domain.booking.performanceSeat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BlockSummaryResponse {

    private Integer floor;
    private String block;
    private long totalSeats;
    private long availableSeats;
}
