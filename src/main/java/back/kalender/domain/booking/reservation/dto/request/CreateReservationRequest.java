package back.kalender.domain.booking.reservation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "예약 세션 생성 요청 (대기열 통과 후)")
public record CreateReservationRequest(
        @Schema(
                description = "대기열 통과 토큰",
                example = "wt_abc123xyz456"
        )
        String waitingToken,

        @Schema(
                description = "디바이스 ID (중복 접속 방지용)",
                example = "device_xyz789"
        )
        String deviceId
) {
}
