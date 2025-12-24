package back.kalender.domain.booking.performanceSeat.scheduler;

import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservation.entity.ReservationStatus;
import back.kalender.domain.booking.reservation.repository.ReservationRepository;
import back.kalender.domain.booking.reservation.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatHoldExpiryScheduler 단위 테스트")
class SeatHoldExpirySchedulerTest {

    @InjectMocks
    private SeatHoldExpiryScheduler scheduler;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationService reservationService;

    @Nested
    @DisplayName("releaseExpiredHolds 테스트")
    class ReleaseExpiredHoldsTest {

        @Test
        @DisplayName("성공: 만료된 HOLD 예약들을 순회하며 expireReservationAndReleaseSeats 호출")
        void releaseExpiredHolds_success() {
            // given
            Reservation r1 = Reservation.builder()
                    .userId(1L)
                    .performanceScheduleId(10L)
                    .status(ReservationStatus.HOLD)
                    .totalAmount(1000)
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .build();
            ReflectionTestUtils.setField(r1, "id", 1L);

            Reservation r2 = Reservation.builder()
                    .userId(2L)
                    .performanceScheduleId(10L)
                    .status(ReservationStatus.HOLD)
                    .totalAmount(2000)
                    .expiresAt(LocalDateTime.now().minusMinutes(2))
                    .build();
            ReflectionTestUtils.setField(r2, "id", 2L);

            given(reservationRepository.findExpiredHoldReservations(any(LocalDateTime.class), any(Pageable.class)))
                    .willReturn(List.of(r1, r2));

            // when
            scheduler.releaseExpiredHolds();

            // then
            verify(reservationService, times(1)).expireReservationAndReleaseSeats(1L, null);
            verify(reservationService, times(1)).expireReservationAndReleaseSeats(2L, null);
        }

        @Test
        @DisplayName("성공: 만료된 예약이 없으면 아무 것도 호출하지 않음")
        void releaseExpiredHolds_empty() {
            // given
            given(reservationRepository.findExpiredHoldReservations(any(LocalDateTime.class), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            scheduler.releaseExpiredHolds();

            // then
            verify(reservationService, never()).expireReservationAndReleaseSeats(anyLong(), any());
        }
    }
}