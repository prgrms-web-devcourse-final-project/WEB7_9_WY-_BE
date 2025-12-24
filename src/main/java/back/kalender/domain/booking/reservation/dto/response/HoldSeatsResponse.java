package back.kalender.domain.booking.reservation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "좌석 선점 응답")
public record HoldSeatsResponse(
        @Schema(description = "예매 ID", example = "123")
        Long reservationId,

        @Schema(description = "예매 상태", example = "HOLD")
        String reservationStatus,

        @Schema(description = "선점된 좌석 목록")
        List<HeldSeatInfo> heldSeats,

        @Schema(description = "총 금액(수수료 제외, 순수 티켓 금액 합)", example = "300000")
        Integer totalAmount,

        @Schema(description = "세션 만료 시각", example = "2026-01-05T14:15:00")
        LocalDateTime expiresAt,

        @Schema(description = "남은 시간(초)", example = "280")
        Long remainingSeconds,

        @Schema(description = "선점된 좌석 개수")
        int heldSeatCount
) {
    @Schema(description = "선점된 좌석 정보")
    public record HeldSeatInfo(
            @Schema(description = "좌석 ID", example = "101")
            Long performanceSeatId,

            @Schema(description = "층", example = "1")
            Integer floor,

            @Schema(description = "구역", example = "A")
            String block,

            @Schema(description = "열", example = "2")
            Integer row,

            @Schema(description = "번호", example = "1")
            Integer number,

            @Schema(description = "가격 등급", example = "VIP")
            String priceGrade,

            @Schema(description = "가격", example = "150000")
            Integer price
    ) {}

}
