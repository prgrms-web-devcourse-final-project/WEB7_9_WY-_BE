package back.kalender.domain.booking.performanceSeat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SubBlockSummaryResponse {
    private final String subBlock;
    private final long total;
    private final long available;

    public static SubBlockSummaryResponse of(String subBlock, long total, long available) {
        return new SubBlockSummaryResponse(subBlock, total, available);
    }
}
