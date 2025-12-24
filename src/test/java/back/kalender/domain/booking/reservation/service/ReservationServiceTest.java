package back.kalender.domain.booking.reservation.service;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.booking.reservation.dto.request.UpdateDeliveryInfoRequest;
import back.kalender.domain.booking.reservation.dto.response.CancelReservationResponse;
import back.kalender.domain.booking.reservation.dto.response.ReservationSummaryResponse;
import back.kalender.domain.booking.reservation.dto.response.UpdateDeliveryInfoResponse;
import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservation.entity.ReservationStatus;
import back.kalender.domain.booking.reservation.mapper.ReservationMapper;
import back.kalender.domain.booking.reservation.repository.ReservationRepository;
import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
import back.kalender.domain.booking.reservationSeat.repository.ReservationSeatRepository;
import back.kalender.domain.booking.seatHold.service.SeatHoldService;
import back.kalender.domain.booking.session.service.BookingSessionService;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService 단위 테스트")
public class ReservationServiceTest {

    @InjectMocks
    private ReservationService reservationService;

    @Spy
    private ReservationMapper reservationMapper = new ReservationMapper();

    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationSeatRepository reservationSeatRepository;
    @Mock private PerformanceScheduleRepository scheduleRepository;
    @Mock private PerformanceRepository performanceRepository;
    @Mock private PerformanceHallRepository hallRepository;
    @Mock private PerformanceSeatRepository performanceSeatRepository;
    @Mock private PriceGradeRepository priceGradeRepository;
    @Mock private SeatHoldService seatHoldService;
    @Mock private BookingSessionService bookingSessionService;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ObjectMapper objectMapper;

    private Reservation reservation;
    private PerformanceSchedule schedule;
    private Performance performance;
    private PerformanceHall hall;
    private PriceGrade priceGrade;
    private PerformanceSeat performanceSeat;
    private ReservationSeat reservationSeat;
    private Artist artist;

    private static final Long USER_ID = 1L;
    private static final Long RESERVATION_ID = 1L;
    private static final Long SCHEDULE_ID = 1L;
    private static final Long PERFORMANCE_ID = 1L;
    private static final Long HALL_ID = 1L;
    private static final Long SEAT_ID = 1L;
    private static final Long PRICE_GRADE_ID = 1L;

    @BeforeEach
    void setUp() {
        artist = new Artist("임영웅", "url");
        ReflectionTestUtils.setField(artist, "id", 1L);

        hall = new PerformanceHall("홀", "주소", "교통");
        ReflectionTestUtils.setField(hall, "id", HALL_ID);

        performance = new Performance(
                HALL_ID, 1L, "공연", "poster",
                LocalDate.now(), LocalDate.now(),
                120, "info",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1)
        );
        ReflectionTestUtils.setField(performance, "id", PERFORMANCE_ID);

        schedule = new PerformanceSchedule(
                PERFORMANCE_ID, LocalDate.now(),
                LocalTime.of(18, 0), 1, ScheduleStatus.AVAILABLE
        );
        ReflectionTestUtils.setField(schedule, "id", SCHEDULE_ID);

        priceGrade = new PriceGrade(PERFORMANCE_ID, "VIP", 200_000);
        ReflectionTestUtils.setField(priceGrade, "id", PRICE_GRADE_ID);

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

        reservationSeat = new ReservationSeat(RESERVATION_ID, SEAT_ID, 200_000);

        reservation = Reservation.builder()
                .userId(USER_ID)
                .performanceScheduleId(SCHEDULE_ID)
                .status(ReservationStatus.HOLD)
                .totalAmount(200_000)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        ReflectionTestUtils.setField(reservation, "id", RESERVATION_ID);
    }

    @Test
    @DisplayName("예매 요약 조회 성공")
    void getReservationSummary_success() {
        given(reservationRepository.findById(RESERVATION_ID))
                .willReturn(Optional.of(reservation));
        given(scheduleRepository.findById(SCHEDULE_ID))
                .willReturn(Optional.of(schedule));
        given(performanceRepository.findById(PERFORMANCE_ID))
                .willReturn(Optional.of(performance));
        given(hallRepository.findById(HALL_ID))
                .willReturn(Optional.of(hall));
        given(reservationSeatRepository.findByReservationId(RESERVATION_ID))
                .willReturn(List.of(reservationSeat));
        given(performanceSeatRepository.findAllById(anyList()))
                .willReturn(List.of(performanceSeat));
        given(priceGradeRepository.findAllById(anySet()))
                .willReturn(List.of(priceGrade));

        ReservationSummaryResponse response =
                reservationService.getReservationSummary(RESERVATION_ID, USER_ID);

        assertThat(response.selectedSeats()).hasSize(1);
        assertThat(response.selectedSeats().get(0).block()).isEqualTo("A");
    }
}