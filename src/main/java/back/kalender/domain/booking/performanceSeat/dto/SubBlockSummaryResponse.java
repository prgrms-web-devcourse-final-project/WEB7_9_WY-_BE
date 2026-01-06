package back.kalender.domain.booking.performanceSeat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SubBlockSummaryResponse {
    private String subBlock;
    private long total;
    private long available;
}
