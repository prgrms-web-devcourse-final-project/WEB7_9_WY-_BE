package back.kalender.domain.booking.reservation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "내 예매 내역 목록 응답")
public record MyReservationListResponse(
        @Schema(description = "예매 목록")
        List<ReservationItem> reservations,

        @Schema(description = "현재 페이지", example = "0")
        int currentPage,

        @Schema(description = "전체 페이지 수", example = "5")
        int totalPages,

        @Schema(description = "전체 예매 수", example = "23")
        long totalElements
) {
    @Schema(description = "예매 항목 (목록용)")
    public record ReservationItem(
            @Schema(description = "예매 ID", example = "1")
            Long reservationId,

            @Schema(description = "예매 번호", example = "M256834798")
            String reservationNumber,

            @Schema(description = "공연 제목", example = "임영웅 IM HERO TOUR 2025")
            String performanceTitle,

            @Schema(description = "공연장명", example = "김대중컨벤션센터")
            String performanceHallName,

            @Schema(description = "공연 날짜", example = "2026-01-03")
            LocalDate performanceDate,

            @Schema(description = "공연 시작 시간", example = "18:00")
            LocalTime startTime,

            @Schema(description = "관람 인원", example = "1매")
            String ticketCount,

            @Schema(description = "예매 상태", example = "예매완료(카카오페이)")
            String statusDisplay,

            @Schema(description = "상태 코드", example = "PAID")
            String status,

            @Schema(description = "취소 가능 기한", example = "2026-01-03 17:00까지")
            String cancelDeadline,

            @Schema(description = "예매일", example = "2025-12-20T15:30:00")
            LocalDateTime createdAt
    ) {}
}