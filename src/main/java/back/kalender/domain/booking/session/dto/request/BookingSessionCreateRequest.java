package back.kalender.domain.booking.session.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "BookingSession 생성 요청")
public record BookingSessionCreateRequest(
        @NotNull(message = "회차 ID는 필수입니다")
        @Schema(description = "공연 회차 ID", example = "1")
        Long scheduleId,

        @NotBlank(message = "대기열 토큰은 필수입니다")
        @Schema(description = "대기열 통과 토큰", example = "wt_abc123xyz")
        String waitingToken,

        @NotBlank(message = "기기 ID는 필수입니다")
        @Schema(description = "기기 식별자", example = "device-xxx")
        String deviceId
) {}