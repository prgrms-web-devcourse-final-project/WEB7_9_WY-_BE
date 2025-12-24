package back.kalender.domain.booking.reservation.service;

import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.booking.reservation.dto.response.MyReservationListResponse;
import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservation.entity.ReservationStatus;
import back.kalender.domain.booking.reservation.mapper.ReservationMapper;
import back.kalender.domain.booking.reservation.repository.ReservationRepository;
import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
import back.kalender.domain.booking.reservationSeat.repository.ReservationSeatRepository;
import back.kalender.domain.performance.performance.entity.Performance;
import back.kalender.domain.performance.performance.repository.PerformanceRepository;
import back.kalender.domain.performance.performanceHall.entity.PerformanceHall;
import back.kalender.domain.performance.performanceHall.repository.PerformanceHallRepository;
import back.kalender.domain.performance.priceGrade.entity.PriceGrade;
import back.kalender.domain.performance.priceGrade.repository.PriceGradeRepository;
import back.kalender.domain.performance.schedule.entity.PerformanceSchedule;
import back.kalender.domain.performance.schedule.entity.ScheduleStatus;
import back.kalender.domain.performance.schedule.repository.PerformanceScheduleRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService 마이페이지 테스트")
public class ReservationServiceMyPageTest {

    @InjectMocks
    private ReservationService reservationService;

    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationSeatRepository reservationSeatRepository;
    @Mock private PerformanceScheduleRepository scheduleRepository;
    @Mock private PerformanceRepository performanceRepository;
    @Mock private PerformanceHallRepository hallRepository;
    @Mock private PerformanceSeatRepository performanceSeatRepository;
    @Mock private PriceGradeRepository priceGradeRepository;
    @Mock private ReservationMapper reservationMapper;

    private static final Long USER_ID = 1L;
    private static final Long RESERVATION_ID = 1L;
    private static final Long SCHEDULE_ID = 1L;
    private static final Long PERFORMANCE_ID = 1L;
    private static final Long HALL_ID = 1L;
    private static final Long SEAT_ID = 1L;
    private static final Long PRICE_GRADE_ID = 1L;

    private Reservation reservation;
    private PerformanceSeat performanceSeat;

    @BeforeEach
    void setUp() {
        performanceSeat = PerformanceSeat.create(
                SCHEDULE_ID,
                1L,
                PRICE_GRADE_ID,
                1,
                "A",
                "A1",   // ✅ subBlock
                1,
                1,
                0,
                1000
        );
        ReflectionTestUtils.setField(performanceSeat, "id", SEAT_ID);

        reservation = Reservation.builder()
                .userId(USER_ID)
                .performanceScheduleId(SCHEDULE_ID)
                .status(ReservationStatus.PAID)
                .totalAmount(200_000)
                .build();
        ReflectionTestUtils.setField(reservation, "id", RESERVATION_ID);
    }

    @Test
    @DisplayName("내 예매 목록 조회 성공")
    void getMyReservations_success() {
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PAID);
    }

    @Test
    @DisplayName("예매 없음 예외")
    void getMyReservations_notFound() {
        given(reservationRepository.findById(any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                reservationService.getReservationDetail(RESERVATION_ID, USER_ID)
        ).isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_NOT_FOUND);
    }
}