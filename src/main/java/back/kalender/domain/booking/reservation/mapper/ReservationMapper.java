package back.kalender.domain.booking.reservation.mapper;

import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.reservation.dto.response.*;
import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservation.entity.ReservationStatus;
import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
import back.kalender.domain.performance.performance.entity.Performance;
import back.kalender.domain.performance.performanceHall.entity.PerformanceHall;
import back.kalender.domain.performance.priceGrade.entity.PriceGrade;
import back.kalender.domain.performance.schedule.entity.PerformanceSchedule;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ReservationMapper {
    private static final int CANCEL_DEADLINE_HOURS = 1;

    // 좌석 홀드 성공 응답 생성
    public static HoldSeatsResponse toHoldSeatsResponse(
            Reservation reservation,
            List<ReservationSeat> reservationSeats,
            Map<Long, PerformanceSeat> performanceSeatMap,
            Map<Long, PriceGrade> priceGradeMap,
            LocalDateTime now
    ) {
        // 선점된 좌석 정보 변환
        List<HoldSeatsResponse.HeldSeatInfo> heldSeats = reservationSeats.stream()
                .map(rs -> {
                    PerformanceSeat seat = performanceSeatMap.get(rs.getPerformanceSeatId());
                    PriceGrade grade = priceGradeMap.get(seat.getPriceGradeId());

                    return new HoldSeatsResponse.HeldSeatInfo(
                            seat.getId(),
                            seat.getFloor(),
                            seat.getBlock(),
                            seat.getRowNumber(),
                            seat.getSeatNumber(),
                            grade.getGradeName(),
                            grade.getPrice()
                    );
                })
                .toList();

        int totalAmount = heldSeats.stream().mapToInt(HoldSeatsResponse.HeldSeatInfo::price).sum();
        long remainingSeconds = calculateRemainingSeconds(reservation, now);

        return new HoldSeatsResponse(
                reservation.getId(),
                reservation.getStatus().name(),
                heldSeats,
                totalAmount,
                reservation.getExpiresAt(),
                remainingSeconds,
                heldSeats.size()
        );
    }

    // 좌석 해제 성공 응답 생성
    public static ReleaseSeatsResponse toReleaseSeatsResponse(
            Reservation reservation,
            List<Long> releasedSeatIds,
            int remainingSeatCount,
            int totalAmount,
            LocalDateTime now
    ) {
        long remainingSeconds = calculateRemainingSeconds(reservation, now);

        return new ReleaseSeatsResponse(
                reservation.getId(),
                reservation.getStatus().name(),
                releasedSeatIds,
                remainingSeatCount,
                totalAmount,
                reservation.getExpiresAt(),
                remainingSeconds,
                releasedSeatIds.size()
        );
    }

    // 예매 요약 응답 생성
    public static ReservationSummaryResponse toSummaryResponse(
            Reservation reservation,
            Performance performance,
            PerformanceSchedule schedule,
            PerformanceHall hall,
            List<ReservationSeat> reservationSeats,
            Map<Long, PerformanceSeat> performanceSeatMap,
            Map<Long, PriceGrade> priceGradeMap,
            LocalDateTime now

    ) {
        // 공연 정보
        var performanceInfo = new ReservationSummaryResponse.PerformanceInfo(
                performance.getId(),
                performance.getTitle(),
                performance.getPosterImageUrl(),
                hall.getName()
        );

        // 회차 정보
        var scheduleInfo = new ReservationSummaryResponse.ScheduleInfo(
                schedule.getId(),
                schedule.getPerformanceDate(),
                schedule.getStartTime(),
                schedule.getPerformanceNo()
        );

        // 선택된 좌석 목록
        List<ReservationSummaryResponse.SelectedSeatInfo> selectedSeats =
                reservationSeats.stream()
                        .map(rs -> {
                            PerformanceSeat seat = performanceSeatMap.get(rs.getPerformanceSeatId());
                            PriceGrade grade = priceGradeMap.get(seat.getPriceGradeId());

                            return new ReservationSummaryResponse.SelectedSeatInfo(
                                    seat.getId(),
                                    seat.getFloor(),
                                    seat.getBlock(),
                                    seat.getRowNumber(),
                                    seat.getSeatNumber(),
                                    grade.getGradeName(),
                                    grade.getPrice()
                            );
                        })
                        .toList();

        // 취소 가능 기한 (공연 시작 1시간 전)
        LocalDateTime cancellationDeadline = schedule.getPerformanceDate()
                .atTime(schedule.getStartTime())
                .minusHours(CANCEL_DEADLINE_HOURS);

        long remainingSeconds = calculateRemainingSeconds(reservation, now);

        return new ReservationSummaryResponse(
                reservation.getId(),
                performanceInfo,
                scheduleInfo,
                selectedSeats,
                reservation.getTotalAmount(),
                reservation.getExpiresAt(),
                remainingSeconds,
                cancellationDeadline
        );
    }

    // 예매 생성
    public CreateReservationResponse toCreateReservationResponse(Reservation reservation) {
        return new CreateReservationResponse(
                reservation.getId(),
                reservation.getStatus().name(),
                reservation.getExpiresAt(),
                0L
        );
    }

    // 배송 정보 입력
    public UpdateDeliveryInfoResponse toUpdateDeliveryInfoResponse(Reservation reservation) {
        long remainingSeconds = reservation.getExpiresAt() != null
                ? calculateRemainingSeconds(reservation, LocalDateTime.now())
                : 0L;

        return new UpdateDeliveryInfoResponse(
                reservation.getId(),
                reservation.getDeliveryMethod(),
                reservation.getUpdatedAt(),
                reservation.getExpiresAt(),
                remainingSeconds
        );
    }

    // 예매 취소
    public CancelReservationResponse toCancelReservationResponse(
            Reservation reservation,
            int cancelledSeatCount
    ) {
        return new CancelReservationResponse(
                reservation.getId(),
                reservation.getStatus().name(),
                reservation.getTotalAmount(),  // 환불 예정 금액
                LocalDateTime.now(),            // 취소 시각
                cancelledSeatCount
        );
    }

    // 예매 내역 목록
    public MyReservationListResponse.ReservationItem toMyReservationItem(
            Reservation reservation,
            PerformanceSchedule schedule,
            Performance performance,
            PerformanceHall hall,
            int seatCount
    ) {
        // 예매 번호 생성 (예: M256834798)
        String reservationNumber = "M" + reservation.getId();

        // 관람 인원 (예: 1매, 3매)
        String ticketCount = seatCount + "매";

        // 상태 표시명
        String statusDisplay = getStatusDisplay(reservation.getStatus());

        // 취소 가능 기한 (PAID일 때만)
        String cancelDeadline = null;
        if (reservation.getStatus() == ReservationStatus.PAID) {
            LocalDateTime deadline = LocalDateTime.of(
                    schedule.getPerformanceDate(),
                    schedule.getStartTime()
            ).minusHours(1);

            cancelDeadline = deadline.format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm까지")
            );
        }

        return new MyReservationListResponse.ReservationItem(
                reservation.getId(),
                reservationNumber,
                performance.getTitle(),
                hall.getName(),
                schedule.getPerformanceDate(),
                schedule.getStartTime(),
                ticketCount,
                statusDisplay,
                reservation.getStatus().name(),
                cancelDeadline,
                reservation.getCreatedAt()
        );
    }

    // 예매 상세
    public ReservationDetailResponse toReservationDetailResponse(
            Reservation reservation,
            PerformanceSchedule schedule,
            Performance performance,
            PerformanceHall hall,
            List<ReservationSeat> reservationSeats,
            Map<Long, PerformanceSeat> seatMap,
            Map<Long, PriceGrade> priceGradeMap
    ) {
        // 1. 예매 정보
        ReservationDetailResponse.ReservationInfo reservationInfo =
                toReservationInfoForDetail(reservation, schedule);

        // 2. 공연 정보
        ReservationDetailResponse.PerformanceInfo performanceInfo =
                toPerformanceInfoForDetail(performance, hall, schedule);

        // 3. 좌석 정보
        List<ReservationDetailResponse.SeatInfo> seats = reservationSeats.stream()
                .map(rs -> {
                    PerformanceSeat seat = Optional.ofNullable(seatMap.get(rs.getPerformanceSeatId()))
                            .orElseThrow(() -> new ServiceException(ErrorCode.PERFORMANCE_SEAT_NOT_FOUND));
                    PriceGrade grade = Optional.ofNullable(priceGradeMap.get(seat.getPriceGradeId()))
                            .orElseThrow(() -> new ServiceException(ErrorCode.PRICE_GRADE_NOT_FOUND));
                    return toSeatInfo(seat, grade, rs.getPrice());
                })
                .toList();

        // 4. 결제 내역
        ReservationDetailResponse.PaymentInfo paymentInfo =
                toPaymentInfo(reservation);

        // 5. 배송/수령 정보
        ReservationDetailResponse.DeliveryInfo deliveryInfo =
                toDeliveryInfo(reservation);

        return new ReservationDetailResponse(
                reservationInfo,
                performanceInfo,
                seats,
                paymentInfo,
                deliveryInfo
        );
    }


    private ReservationDetailResponse.ReservationInfo toReservationInfoForDetail(
            Reservation reservation,
            PerformanceSchedule schedule
    ) {
        String reservationNumber = "M" + reservation.getId();
        String statusDisplay = getStatusDisplay(reservation.getStatus());

        boolean cancellable = false;
        LocalDateTime cancelDeadline = null;

        if (reservation.getStatus() == ReservationStatus.PAID) {
            cancelDeadline = LocalDateTime.of(
                    schedule.getPerformanceDate(),
                    schedule.getStartTime()
            ).minusHours(1);

            cancellable = LocalDateTime.now().isBefore(cancelDeadline);
        }

        return new ReservationDetailResponse.ReservationInfo(
                reservation.getId(),
                reservationNumber,
                reservation.getStatus().name(),
                statusDisplay,
                reservation.getCreatedAt(),
                cancellable,
                cancelDeadline
        );
    }

    // 공연 정보 변환 (상세)
    private ReservationDetailResponse.PerformanceInfo toPerformanceInfoForDetail(
            Performance performance,
            PerformanceHall hall,
            PerformanceSchedule schedule
    ) {
        String round = schedule.getPerformanceNo() + "회차";

        return new ReservationDetailResponse.PerformanceInfo(
                performance.getId(),
                performance.getTitle(),
                hall.getName(),
                schedule.getPerformanceDate(),
                schedule.getStartTime(),
                round
        );
    }

    // 좌석 정보 변환 (상세)
    private ReservationDetailResponse.SeatInfo toSeatInfo(
            PerformanceSeat seat,
            PriceGrade grade,
            Integer price
    ) {
        String floor = seat.getFloor() + "층";
        String block = seat.getBlock() + "블록";
        String row = seat.getRowNumber() + "열";
        String seatNumber = seat.getSeatNumber() + "번";

        return new ReservationDetailResponse.SeatInfo(
                seat.getId(),
                floor,
                block,
                row,
                seatNumber,
                grade.getGradeName(),
                price
        );
    }

    // 결제 내역 변환 (상세)
    private ReservationDetailResponse.PaymentInfo toPaymentInfo(Reservation reservation) {
        // TODO: 결제 API 연동 후 실제 결제 수단, 결제일시 채우기
        return new ReservationDetailResponse.PaymentInfo(
                reservation.getTotalAmount(),
                "토스페이",  // TODO: 실제 결제 수단
                reservation.getCreatedAt(),  // TODO: 실제 결제일시
                "결제 API 연동 후 추가 예정"
        );
    }

    // 배송/수령 정보 변환 (상세)
    private ReservationDetailResponse.DeliveryInfo toDeliveryInfo(Reservation reservation) {
        String deliveryMethodDisplay = "DELIVERY".equals(reservation.getDeliveryMethod())
                ? "배송" : "현장수령";

        return new ReservationDetailResponse.DeliveryInfo(
                deliveryMethodDisplay,
                reservation.getRecipientName(),
                reservation.getRecipientPhone(),
                reservation.getRecipientAddress()
        );
    }

    // 상태 표시명 반환
    private String getStatusDisplay(ReservationStatus status) {
        return switch (status) {
            case PAID -> "예매완료(토스페이)";  // TODO: 실제 결제 수단 반영
            case CANCELLED -> "취소완료";
            default -> status.name();
        };
    }

    // 남은 시간(초) 계산
    public static long calculateRemainingSeconds(Reservation reservation, LocalDateTime now) {
        if (reservation.getExpiresAt() == null) {
            return 0L;
        }
        long seconds = Duration.between(now, reservation.getExpiresAt()).getSeconds();
        return Math.max(0, seconds);
    }
}
