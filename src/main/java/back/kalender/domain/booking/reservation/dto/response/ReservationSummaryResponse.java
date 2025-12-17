package back.kalender.domain.booking.reservation.dto.response;

import back.kalender.domain.booking.reservation.entity.Reservation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "예약 요약 정보 (Step3 화면용)")
public record ReservationSummaryResponse(
        @Schema(description = "예약 ID", example = "123")
        Long reservationId,

        @Schema(description = "공연 정보")
        PerformanceInfo performance,

        @Schema(description = "회차 정보")
        ScheduleInfo schedule,

        @Schema(description = "선택된 좌석 목록")
        List<SelectedSeatInfo> selectedSeats,

        @Schema(description = "총 금액", example = "300000")
        Integer totalAmount,

        @Schema(description = "세션 만료 시각", example = "2026-01-05T14:15:00")
        LocalDateTime expiresAt,

        @Schema(description = "남은 시간(초)", example = "240")
        Long remainingSeconds,

        @Schema(description = "취소 가능 기한", example = "2026-01-05T17:00:00")
        LocalDateTime cancellationDeadline
) {
    @Schema(description = "공연 정보")
    public record PerformanceInfo(
            @Schema(description = "공연 ID", example = "1")
            Long performanceId,

            @Schema(description = "공연 제목", example = "KPOP DREAM CONCERT")
            String title,

            @Schema(description = "포스터 이미지 URL", example = "https://example.com/poster.jpg")
            String posterImageUrl,

            @Schema(description = "공연장명", example = "KPOP Arena Hall")
            String performanceHallName
    ) {
    }

    @Schema(description = "회차 정보")
    public record ScheduleInfo(
            @Schema(description = "회차 ID", example = "1")
            Long scheduleId,

            @Schema(description = "공연 날짜", example = "2026-01-05")
            LocalDate performanceDate,

            @Schema(description = "시작 시간", example = "18:00")
            LocalTime startTime,

            @Schema(description = "회차 번호", example = "1")
            Integer performanceNo
    ) {
    }

    @Schema(description = "선택된 좌석 정보")
    public record SelectedSeatInfo(
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
    ) {
    }
}
