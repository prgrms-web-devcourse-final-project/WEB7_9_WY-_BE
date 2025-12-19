package back.kalender.domain.booking.reservation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "좌석 선점 해제 요청")
public record ReleaseSeatsRequest(
        @Schema(description = "해제할 좌석 ID 목록", example = "[101]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "해제할 좌석을 선택해주세요")
        List<Long> performanceSeatIds
) {
}
