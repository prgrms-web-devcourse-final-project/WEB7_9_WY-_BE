package back.kalender.domain.booking.reservation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "좌석 선점 해제 응답")
public record ReleaseSeatsResponse(
        @Schema(description = "예매 ID", example = "123")
        Long reservationId,

        @Schema(description = "예매 상태", example = "HOLD")
        String reservationStatus,

        @Schema(description = "해제된 좌석 ID 목록", example = "[101]")
        List<Long> releasedSeatIds,

        @Schema(description = "해제된 좌석 수", example = "2")
        Integer remainingSeatCount,

        @Schema(description = "총 금액(수수료 제외, 순수 티켓 금액 합)", example = "150000")
        Integer totalAmount,

        @Schema(description = "세션 만료 시각", example = "2026-01-05T14:15:00")
        LocalDateTime expiresAt,

        @Schema(description = "남은 시간(초)", example = "260")
        Long remainingSeconds,

        @Schema(description = "해제된 좌석 개수")
        int releasedSeatCount
) {
}
