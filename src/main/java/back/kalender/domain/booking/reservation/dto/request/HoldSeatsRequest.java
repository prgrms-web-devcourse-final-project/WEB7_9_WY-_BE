package back.kalender.domain.booking.reservation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "좌석 선점 요청")
public record HoldSeatsRequest(
        @Schema(description = "선점할 좌석 ID 목록", example = "[101, 102, 103]")
        @NotEmpty(message = "최소 1개의 좌석을 선택해야 합니다.")
        @Size(max=4, message = "최대 4개의 좌석까지 선택할 수 있습니다.")
        List<Long> performanceSeatIds
) {
}
