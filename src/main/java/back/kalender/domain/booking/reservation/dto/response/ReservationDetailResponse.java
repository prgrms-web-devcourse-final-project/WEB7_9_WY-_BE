package back.kalender.domain.booking.reservation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "예매 상세 응답")
public record ReservationDetailResponse(
        @Schema(description = "예매 정보")
        ReservationInfo reservationInfo,

        @Schema(description = "공연 정보")
        PerformanceInfo performanceInfo,

        @Schema(description = "좌석 정보")
        List<SeatInfo> seats,

        @Schema(description = "결제 내역")
        PaymentInfo paymentInfo,

        @Schema(description = "배송/수령 정보")
        DeliveryInfo deliveryInfo
) {
    @Schema(description = "예매 정보")
    public record ReservationInfo(
            @Schema(description = "예매 ID", example = "1")
            Long reservationId,

            @Schema(description = "예매 번호", example = "M256834798")
            String reservationNumber,

            @Schema(description = "예매 상태", example = "PAID")
            String status,

            @Schema(description = "상태 표시명", example = "예매완료(카카오페이)")
            String statusDisplay,

            @Schema(description = "예매일", example = "2025-12-20T15:30:00")
            LocalDateTime createdAt,

            @Schema(description = "취소 가능 여부", example = "true")
            boolean cancellable,

            @Schema(description = "취소 가능 기한", example = "2026-01-03T17:00:00")
            LocalDateTime cancelDeadline
    ) {}

    @Schema(description = "공연 정보")
    public record PerformanceInfo(
            @Schema(description = "공연 ID", example = "1")
            Long performanceId,

            @Schema(description = "공연 제목", example = "임영웅 IM HERO TOUR 2025")
            String title,

            @Schema(description = "공연장명", example = "김대중컨벤션센터")
            String performanceHallName,

            @Schema(description = "공연 날짜", example = "2026-01-03")
            LocalDate performanceDate,

            @Schema(description = "시작 시간", example = "18:00")
            LocalTime startTime,

            @Schema(description = "회차", example = "1회차")
            String performanceNo
    ) {}

    @Schema(description = "좌석 정보")
    public record SeatInfo(
            @Schema(description = "좌석 ID", example = "1")
            Long seatId,

            @Schema(description = "층", example = "1층")
            String floor,

            @Schema(description = "블록", example = "A블록")
            String block,

            @Schema(description = "열", example = "1열")
            String row,

            @Schema(description = "좌석 번호", example = "1번")
            String seatNumber,

            @Schema(description = "등급", example = "VIP")
            String grade,

            @Schema(description = "가격", example = "200000")
            Integer price
    ) {}

    @Schema(description = "결제 내역")
    public record PaymentInfo(
            @Schema(description = "총 결제 금액", example = "600000")
            Integer totalAmount,

            @Schema(description = "결제 수단", example = "카카오페이")
            String paymentMethod,

            @Schema(description = "결제일시", example = "2025-12-20T15:35:00")
            LocalDateTime paidAt,

            @Schema(description = "TODO: 결제 API 연동 후 추가 예정")
            String note
    ) {}

    @Schema(description = "배송/수령 정보")
    public record DeliveryInfo(
            @Schema(description = "수령 방법", example = "배송")
            String deliveryMethod,

            @Schema(description = "수령인", example = "홍길동")
            String recipientName,

            @Schema(description = "연락처", example = "010-1234-5678")
            String recipientPhone,

            @Schema(description = "주소", example = "서울시 강남구 테헤란로 123")
            String address
    ) {}
}