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
    void setUp() throws Exception {
        // Redis Mock 설정
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(redisTemplate.opsForSet()).willReturn(setOps);

        // Redisson Lock Mock 설정
        given(redissonClient.getLock(anyString())).willReturn(lock);
        given(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);

        // Reservation 생성
        reservation = Reservation.builder()
                .userId(USER_ID)
                .performanceScheduleId(SCHEDULE_ID)
                .status(ReservationStatus.PENDING)
                .totalAmount(0)
                .build();
        ReflectionTestUtils.setField(reservation, "id", RESERVATION_ID);

        // PerformanceSeat 생성
        seat = PerformanceSeat.create(
                SCHEDULE_ID, 1L, PRICE_GRADE_ID,
                1, "A", 1, 1, 10, 10
        );
        ReflectionTestUtils.setField(seat, "id", SEAT_ID);

        // PriceGrade 생성
        priceGrade = PriceGrade.builder()
                .performanceId(1L)
                .gradeName("VIP")
                .price(100_000)
                .build();
        ReflectionTestUtils.setField(priceGrade, "id", PRICE_GRADE_ID);

        // ReservationSeat 생성
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

            given(reservationSeatRepository.findByReservationId(RESERVATION_ID))
                    .willReturn(List.of(reservationSeat));

            // Redis SOLD 체크 (SOLD 아님)
            given(setOps.isMember(anyString(), anyString())).willReturn(false);

            // Redis HOLD owner 체크 (없음)
            given(valueOps.get(anyString())).willReturn(null);
            given(priceGradeRepository.findById(PRICE_GRADE_ID))
                    .willReturn(Optional.of(priceGrade));
            given(performanceSeatRepository.save(any(PerformanceSeat.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(reservationSeatRepository.save(any(ReservationSeat.class)))
                    .willReturn(reservationSeat);
            given(seatHoldLogRepository.save(any(SeatHoldLog.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // Reservation save (상태 업데이트 후)
            given(reservationRepository.save(any(Reservation.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // ReservationMapper에서 필요한 PerformanceSeat 조회
            given(performanceSeatRepository.findAllById(List.of(SEAT_ID)))
                    .willReturn(List.of(seat));

            // ReservationSeat 조회 (총액 계산용)
            given(reservationSeatRepository.findByReservationId(RESERVATION_ID))
                    .willReturn(List.of(reservationSeat));

            HoldSeatsResponse response =
                    seatHoldService.holdSeats(RESERVATION_ID, request, USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.reservationId()).isEqualTo(RESERVATION_ID);
            assertThat(response.reservationStatus()).isEqualTo("HOLD");
            assertThat(response.heldSeats()).isNotEmpty();

            verify(performanceSeatRepository).save(any(PerformanceSeat.class));
            verify(reservationSeatRepository).save(any(ReservationSeat.class));
            verify(seatHoldLogRepository).save(any(SeatHoldLog.class));
            verify(reservationRepository).save(any(Reservation.class));
            verify(lock).unlock();
        }

        @Test
        @DisplayName("실패: 이미 SOLD 좌석")
        void holdSeats_alreadySold() {
            HoldSeatsRequest request = new HoldSeatsRequest(List.of(SEAT_ID));

            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));

            given(performanceSeatRepository.findByIdAndScheduleId(SEAT_ID, SCHEDULE_ID))
                    .willReturn(Optional.of(seat));

            // Redis SOLD set에 존재
            given(setOps.isMember(anyString(), anyString())).willReturn(true);

            org.junit.jupiter.api.Assertions.assertThrows(
                    Exception.class, // 실제 Exception 타입으로 변경 필요
                    () -> seatHoldService.holdSeats(RESERVATION_ID, request, USER_ID)
            );

            verify(lock).unlock();
        }

        @Test
        @DisplayName("실패: 이미 다른 사용자가 HOLD 중")
        void holdSeats_alreadyHeld() {
            HoldSeatsRequest request = new HoldSeatsRequest(List.of(SEAT_ID));

            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));

            given(performanceSeatRepository.findByIdAndScheduleId(SEAT_ID, SCHEDULE_ID))
                    .willReturn(Optional.of(seat));

            given(setOps.isMember(anyString(), anyString())).willReturn(false);

            // 다른 사용자가 HOLD 중
            given(valueOps.get(anyString())).willReturn("999");

            org.junit.jupiter.api.Assertions.assertThrows(
                    Exception.class,
                    () -> seatHoldService.holdSeats(RESERVATION_ID, request, USER_ID)
            );

            verify(lock).unlock();
        }
    }

    // ================= RELEASE =================

    @Nested
    @DisplayName("releaseSeats 테스트")
    class ReleaseSeatsTest {

        @BeforeEach
        void setUpRelease() {
            // HOLD 상태로 설정
            seat.updateStatus(SeatStatus.HOLD);
            seat.updateHoldInfo(USER_ID, LocalDateTime.now().plusMinutes(7));

            // Reservation도 HOLD 상태로
            reservation.updateStatus(ReservationStatus.HOLD);
        }

        @Test
        @DisplayName("성공: 전체 좌석 RELEASE")
        void releaseSeats_success() {
            ReleaseSeatsRequest request = new ReleaseSeatsRequest(List.of(SEAT_ID));

            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));

            // 전체 좌석 조회 (전체 해제 검증용)
            given(reservationSeatRepository.findByReservationId(RESERVATION_ID))
                    .willReturn(List.of(reservationSeat));

            given(performanceSeatRepository.findByIdAndScheduleId(SEAT_ID, SCHEDULE_ID))
                    .willReturn(Optional.of(seat));

            // Redis HOLD owner 확인 (본인)
            given(valueOps.get(anyString())).willReturn(USER_ID.toString());

            given(performanceSeatRepository.save(any(PerformanceSeat.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(reservationRepository.save(any(Reservation.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(seatHoldLogRepository.save(any(SeatHoldLog.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            given(redisTemplate.delete(anyString())).willReturn(true);

            ReleaseSeatsResponse response =
                    seatHoldService.releaseSeats(RESERVATION_ID, request, USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.reservationId()).isEqualTo(RESERVATION_ID);
            assertThat(response.releasedSeatIds()).contains(SEAT_ID);

            verify(performanceSeatRepository).save(any(PerformanceSeat.class));
            verify(reservationSeatRepository).deleteByReservationId(RESERVATION_ID);
            verify(lock).unlock();
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

            @Test
            @DisplayName("sinceVersion >= currentVersion → 빈 리스트")
            void noChanges() {
                given(valueOps.get("seat:version:" + SCHEDULE_ID)).willReturn("5");

                List<Map<String, Object>> result =
                        seatHoldService.getSeatChanges(SCHEDULE_ID, 5L);

                assertThat(result).isEmpty();
            }

            @Test
            @DisplayName("gap > 100 → FULL_REFRESH_REQUIRED")
            void fullRefreshRequired() {
                given(valueOps.get("seat:version:" + SCHEDULE_ID)).willReturn("200");

                List<Map<String, Object>> result =
                        seatHoldService.getSeatChanges(SCHEDULE_ID, 0L);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).get("type"))
                        .isEqualTo("FULL_REFRESH_REQUIRED");
            }

            @Test
            @DisplayName("정상 범위 → 변경 이벤트 반환")
            void getChanges_success() {
                given(valueOps.get("seat:version:" + SCHEDULE_ID)).willReturn("1");

                String changeKey = "seat:changes:" + SCHEDULE_ID + ":1";
                given(valueOps.get(changeKey))
                        .willReturn("{\"seatId\":1,\"status\":\"HOLD\",\"userId\":1}");

                List<Map<String, Object>> result =
                        seatHoldService.getSeatChanges(SCHEDULE_ID, 0L);

                assertThat(result).hasSize(1);
                assertThat(result.get(0)).containsEntry("seatId", 1);
                assertThat(result.get(0)).containsEntry("status", "HOLD");
                assertThat(result.get(0)).containsKey("version");
            }
        }
    }
}