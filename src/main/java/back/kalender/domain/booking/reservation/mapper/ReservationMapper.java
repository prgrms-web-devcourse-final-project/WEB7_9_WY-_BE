package back.kalender.domain.booking.reservation.mapper;

import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.reservation.dto.response.HoldSeatsResponse;
import back.kalender.domain.booking.reservation.dto.response.ReleaseSeatsResponse;
import back.kalender.domain.booking.reservation.dto.response.ReservationSummaryResponse;
import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
import back.kalender.domain.performance.performance.entity.Performance;
import back.kalender.domain.performance.performanceHall.entity.PerformanceHall;
import back.kalender.domain.performance.priceGrade.entity.PriceGrade;
import back.kalender.domain.performance.schedule.entity.PerformanceSchedule;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReservationMapper {

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
                remainingSeconds
        );
    }

    // 좌석 홀드 해제 성공 응답 생성
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
                remainingSeconds
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
                .minusHours(1);

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

    // 남은 시간(초) 계산
    public static long calculateRemainingSeconds(Reservation reservation, LocalDateTime now) {
        long seconds = Duration.between(now, reservation.getExpiresAt()).getSeconds();
        return Math.max(0, seconds);
    }
}
