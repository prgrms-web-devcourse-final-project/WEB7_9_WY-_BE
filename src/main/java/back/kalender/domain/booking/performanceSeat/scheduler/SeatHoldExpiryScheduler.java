package back.kalender.domain.booking.performanceSeat.scheduler;

import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservation.entity.ReservationStatus;
import back.kalender.domain.booking.reservation.repository.ReservationRepository;
import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
import back.kalender.domain.booking.reservationSeat.repository.ReservationSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldExpiryScheduler {
    private static final int BATCH_SIZE = 500;

    private final PerformanceSeatRepository performanceSeatRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final ReservationRepository reservationRepository;

    /**
     * 만료된 HOLD 좌석을 주기적으로 해제하는 스케줄러
     * - 10초마다 실행
     */

    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void releaseExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();

        List<PerformanceSeat> expiredSeats =
                performanceSeatRepository.findExpiredHoldSeats(
                        SeatStatus.HOLD,
                        now,
                        PageRequest.of(0, BATCH_SIZE)
                );

        if (expiredSeats.isEmpty()) {
            return;
        }

        // 1) 좌석 복구
        for (PerformanceSeat seat : expiredSeats) {
            seat.updateStatus(SeatStatus.AVAILABLE);
            seat.clearHoldInfo();
            log.info("[SeatHoldExpiryScheduler] 만료된 좌석 해제 - seatId={}", seat.getId());
        }
        performanceSeatRepository.saveAll(expiredSeats);

        // 2) 연결된 reservation EXPIRED 처리 (HOLD/PENDING만)
        Set<Long> seatIds = expiredSeats.stream()
                .map(PerformanceSeat::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        int expiredReservationCount = 0;

        if (!seatIds.isEmpty()) {
            List<ReservationSeat> rsList = reservationSeatRepository.findByPerformanceSeatIdIn(seatIds);

            Set<Long> reservationIds = rsList.stream()
                    .map(ReservationSeat::getReservationId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (!reservationIds.isEmpty()) {
                List<Reservation> reservations = reservationRepository.findAllById(reservationIds);

                for (Reservation r : reservations) {
                    if (r.getStatus() == ReservationStatus.HOLD) {
                        r.expire();
                        expiredReservationCount++;
                    }
                }
                reservationRepository.saveAll(reservations);
            }
        }

        log.info("[SeatHoldExpiryScheduler] 만료된 좌석 홀드 해제 - seatCount={}, reservationExpiredCount={}",
                expiredSeats.size(), expiredReservationCount);
    }
}
