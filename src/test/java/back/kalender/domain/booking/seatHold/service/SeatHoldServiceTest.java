package back.kalender.domain.booking.seatHold.service;

import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.booking.reservation.dto.request.HoldSeatsRequest;
import back.kalender.domain.booking.reservation.dto.request.ReleaseSeatsRequest;
import back.kalender.domain.booking.reservation.dto.response.HoldSeatsResponse;
import back.kalender.domain.booking.reservation.dto.response.ReleaseSeatsResponse;
import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservation.entity.ReservationStatus;
import back.kalender.domain.booking.reservation.repository.ReservationRepository;
import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
import back.kalender.domain.booking.reservationSeat.repository.ReservationSeatRepository;
import back.kalender.domain.booking.seatHold.entity.SeatHoldLog;
import back.kalender.domain.booking.seatHold.repository.SeatHoldLogRepository;
import back.kalender.domain.performance.priceGrade.entity.PriceGrade;
import back.kalender.domain.performance.priceGrade.repository.PriceGradeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SeatHoldService 단위 테스트")
class SeatHoldServiceTest {

    @InjectMocks
    private SeatHoldService seatHoldService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PerformanceSeatRepository performanceSeatRepository;

    @Mock
    private ReservationSeatRepository reservationSeatRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SeatHoldLogRepository seatHoldLogRepository;

    @Mock
    private PriceGradeRepository priceGradeRepository;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private SetOperations<String, String> setOps;

    @Mock
    private RLock lock;

    private static final Long USER_ID = 1L;
    private static final Long SCHEDULE_ID = 100L;
    private static final Long RESERVATION_ID = 200L;
    private static final Long SEAT_ID = 10L;
    private static final Long PRICE_GRADE_ID = 7L;

    private Reservation reservation;
    private PerformanceSeat seat;
    private PriceGrade priceGrade;
    private ReservationSeat reservationSeat;

    @BeforeEach
    void setUp() throws InterruptedException { // ✅ 여기만 추가
        // Redis Mock
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(redisTemplate.opsForSet()).willReturn(setOps);

        // Redisson Lock Mock
        given(redissonClient.getLock(anyString())).willReturn(lock);
        given(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);

        // Reservation
        reservation = Reservation.builder()
                .userId(USER_ID)
                .performanceScheduleId(SCHEDULE_ID)
                .status(ReservationStatus.PENDING)
                .totalAmount(0)
                .build();
        ReflectionTestUtils.setField(reservation, "id", RESERVATION_ID);

        // PerformanceSeat (subBlock 추가됨)
        seat = PerformanceSeat.create(
                SCHEDULE_ID,
                1L,
                PRICE_GRADE_ID,
                1,
                "A",
                "A1",
                1,
                1,
                10,
                10
        );
        ReflectionTestUtils.setField(seat, "id", SEAT_ID);

        // PriceGrade
        priceGrade = PriceGrade.builder()
                .performanceId(1L)
                .gradeName("VIP")
                .price(100_000)
                .build();
        ReflectionTestUtils.setField(priceGrade, "id", PRICE_GRADE_ID);

        // ReservationSeat
        reservationSeat = ReservationSeat.builder()
                .reservationId(RESERVATION_ID)
                .performanceSeatId(SEAT_ID)
                .price(100_000)
                .build();
        ReflectionTestUtils.setField(reservationSeat, "id", 1L);
    }

    // ================= HOLD =================

    @Nested
    @DisplayName("holdSeats 테스트")
    class HoldSeatsTest {

        @Test
        @DisplayName("성공: 단일 좌석 HOLD")
        void holdSeats_success() {
            HoldSeatsRequest request = new HoldSeatsRequest(List.of(SEAT_ID));

            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));
            given(performanceSeatRepository.findByIdAndScheduleId(SEAT_ID, SCHEDULE_ID))
                    .willReturn(Optional.of(seat));
            given(performanceSeatRepository.findAllById(List.of(SEAT_ID)))
                    .willReturn(List.of(seat));
            given(priceGradeRepository.findAllById(Set.of(PRICE_GRADE_ID)))
                    .willReturn(List.of(priceGrade));
            given(priceGradeRepository.findById(PRICE_GRADE_ID))
                    .willReturn(Optional.of(priceGrade));
            given(reservationSeatRepository.findByReservationId(RESERVATION_ID))
                    .willReturn(List.of(reservationSeat));

            given(setOps.isMember(anyString(), anyString())).willReturn(false);
            given(valueOps.get(anyString())).willReturn(null);

            given(performanceSeatRepository.save(any(PerformanceSeat.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            given(reservationSeatRepository.save(any(ReservationSeat.class)))
                    .willReturn(reservationSeat);
            given(seatHoldLogRepository.save(any(SeatHoldLog.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            given(reservationRepository.save(any(Reservation.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            HoldSeatsResponse response =
                    seatHoldService.holdSeats(RESERVATION_ID, request, USER_ID);

            assertThat(response.reservationId()).isEqualTo(RESERVATION_ID);
            assertThat(response.reservationStatus()).isEqualTo("HOLD");

            verify(lock).unlock();
        }
    }

    // ================= RELEASE =================

    @Nested
    @DisplayName("releaseSeats 테스트")
    class ReleaseSeatsTest {

        @BeforeEach
        void setUpRelease() {
            seat.updateStatus(SeatStatus.HOLD);
            seat.updateHoldInfo(USER_ID, LocalDateTime.now().plusMinutes(5));
            reservation.updateStatus(ReservationStatus.HOLD);
        }

        @Test
        @DisplayName("성공: 전체 좌석 RELEASE")
        void releaseSeats_success() {
            ReleaseSeatsRequest request = new ReleaseSeatsRequest(List.of(SEAT_ID));

            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));
            given(reservationSeatRepository.findByReservationId(RESERVATION_ID))
                    .willReturn(List.of(reservationSeat));
            given(performanceSeatRepository.findByIdAndScheduleId(SEAT_ID, SCHEDULE_ID))
                    .willReturn(Optional.of(seat));
            given(valueOps.get(anyString())).willReturn(USER_ID.toString());

            given(performanceSeatRepository.save(any()))
                    .willAnswer(inv -> inv.getArgument(0));
            given(reservationRepository.save(any()))
                    .willAnswer(inv -> inv.getArgument(0));
            given(seatHoldLogRepository.save(any()))
                    .willAnswer(inv -> inv.getArgument(0));

            ReleaseSeatsResponse response =
                    seatHoldService.releaseSeats(RESERVATION_ID, request, USER_ID);

            assertThat(response.releasedSeatIds()).contains(SEAT_ID);
            verify(lock).unlock();
        }
    }

    // ================= POLLING =================

    @Nested
    @DisplayName("getSeatChanges 테스트")
    class GetSeatChangesTest {

        @Test
        @DisplayName("버전 키 없음 → 빈 리스트")
        void noVersionKey() {
            given(valueOps.get("seat:version:" + SCHEDULE_ID)).willReturn(null);

            List<Map<String, Object>> result =
                    seatHoldService.getSeatChanges(SCHEDULE_ID, 0L);

            assertThat(result).isEmpty();
        }
    }
}