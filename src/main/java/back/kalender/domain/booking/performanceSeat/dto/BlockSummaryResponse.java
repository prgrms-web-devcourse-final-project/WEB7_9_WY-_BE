package back.kalender.domain.booking.performanceSeat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BlockSummaryResponse {
    private final int floor;
    private final String block;
    private final long totalSeats;
    private final long availableSeats;

    public static BlockSummaryResponse of(int floor, String block, long total, long available) {
        return new BlockSummaryResponse(floor, block, total, available);
    }
}
