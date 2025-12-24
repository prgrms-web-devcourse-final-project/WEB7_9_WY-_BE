package back.kalender.domain.booking.performanceSeat.scheduler;

import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservation.entity.ReservationStatus;
import back.kalender.domain.booking.reservation.repository.ReservationRepository;
import back.kalender.domain.booking.reservation.service.ReservationService;
import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
import back.kalender.domain.booking.reservationSeat.repository.ReservationSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
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
    private static final int BATCH_SIZE = 100;

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    /**
     * 만료된 HOLD 예매를 주기적으로 해제하는 스케줄러
     * - 10초마다 실행
     */
    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void releaseExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 만료된 예매 조회
        List<Reservation> expiredReservations =
                reservationRepository.findExpiredHoldReservations(
                        now,
                        PageRequest.of(0, BATCH_SIZE)
                );

        if (expiredReservations.isEmpty()) {
            return;
        }

        // 2. 각 예매별로 좌석 해제 (Service 메서드 재사용)
        int successCount = 0;
        int failCount = 0;

        for (Reservation reservation : expiredReservations) {
            try {
                //
                reservationService.expireReservationAndReleaseSeats(
                        reservation.getId(),
                        null
                );
                successCount++;

            } catch (Exception e) {
                log.error("[SeatHoldExpiryScheduler] 예매 만료 처리 실패 - reservationId={}",
                        reservation.getId(), e);
                failCount++;
            }
        }

        log.info("[SeatHoldExpiryScheduler] 만료 처리 완료 - " +
                        "total={}, success={}, fail={}",
                expiredReservations.size(), successCount, failCount);
    }
}
