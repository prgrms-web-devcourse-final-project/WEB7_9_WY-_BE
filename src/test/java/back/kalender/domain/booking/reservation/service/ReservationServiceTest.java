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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService 단위 테스트")
public class ReservationServiceTest {
    @InjectMocks
    private ReservationService reservationService;
    @Spy
    private ReservationMapper reservationMapper = new ReservationMapper();

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ReservationSeatRepository reservationSeatRepository;
    @Mock
    private PerformanceScheduleRepository scheduleRepository;
    @Mock
    private PerformanceRepository performanceRepository;
    @Mock
    private PerformanceHallRepository hallRepository;
    @Mock
    private PerformanceSeatRepository performanceSeatRepository;
    @Mock
    private PriceGradeRepository priceGradeRepository;
    @Mock
    private SeatHoldService seatHoldService;
    @Mock
    BookingSessionService bookingSessionService;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ObjectMapper objectMapper;

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
    private static final Long ARTIST_ID = 1L;

    @BeforeEach
    void setUp() {
        artist = new Artist("임영웅", "https://example.com/artist.jpg");
        setId(artist, ARTIST_ID);

        hall = new PerformanceHall(
                "김대중컨벤션센터",
                "광주광역시 서구 내방로 111",
                "지하철 1호선 김대중컨벤션센터역 3번 출구"
        );
        setId(hall, HALL_ID);

        performance = new Performance(
                hall.getId(),
                artist.getId(),
                "임영웅 IM HERO TOUR 2025 - 광주",
                "https://example.com/poster.jpg",
                LocalDate.of(2026, 1, 3),
                LocalDate.of(2026, 1, 4),
                150,
                "※ 11/24(월)~28(금) 일괄배송 예정입니다.",
                LocalDateTime.of(2025, 9, 23, 14, 0),
                LocalDateTime.of(2026, 1, 4, 23, 59)
        );
        setId(performance, PERFORMANCE_ID);

        schedule = new PerformanceSchedule(
                performance.getId(),
                LocalDate.of(2026, 1, 3),
                LocalTime.of(18, 0),
                1,
                ScheduleStatus.AVAILABLE
        );
        setId(schedule, SCHEDULE_ID);


        priceGrade = new PriceGrade(performance.getId(), "VIP", 200_000);
        setId(priceGrade, PRICE_GRADE_ID);

        performanceSeat = PerformanceSeat.create(
                SCHEDULE_ID,
                1L,  // hallSeatId
                PRICE_GRADE_ID,
                1,   // floor
                "A", // block
                1,   // row
                1,   // number
                0,   // x
                1000 // y
        );
        setId(performanceSeat, SEAT_ID);

        reservationSeat = new ReservationSeat(RESERVATION_ID, performanceSeat.getId(), 200_000);
        setId(reservationSeat, 1L);

        reservation = Reservation.builder()
                .userId(USER_ID)
                .performanceScheduleId(SCHEDULE_ID)
                .status(ReservationStatus.HOLD)
                .totalAmount(200_000)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        setId(reservation, RESERVATION_ID);
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            try {
                java.lang.reflect.Field field = entity.getClass().getSuperclass().getDeclaredField("id");
                field.setAccessible(true);
                field.set(entity, id);
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                throw new RuntimeException("Failed to set ID", ex);
            }
        }
    }


    @Nested
    @DisplayName("getReservationSummary 테스트")
    class GetReservationSummaryTest {

        @Test
        @DisplayName("성공: PENDING 상태 - 좌석 정보 없음")
        void getReservationSummary_Success_PendingStatus() {
            reservation = Reservation.builder()
                    .userId(USER_ID)
                    .performanceScheduleId(SCHEDULE_ID)
                    .status(ReservationStatus.PENDING)
                    .totalAmount(0)
                    .expiresAt(null)
                    .build();
            setId(reservation, RESERVATION_ID);

            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));
            given(scheduleRepository.findById(SCHEDULE_ID))
                    .willReturn(Optional.of(schedule));
            given(performanceRepository.findById(PERFORMANCE_ID))
                    .willReturn(Optional.of(performance));
            given(hallRepository.findById(HALL_ID))
                    .willReturn(Optional.of(hall));
            given(reservationSeatRepository.findByReservationId(RESERVATION_ID))
                    .willReturn(List.of());

            ReservationSummaryResponse response = reservationService.getReservationSummary(
                    RESERVATION_ID, USER_ID
            );

            assertThat(response).isNotNull();
            assertThat(response.reservationId()).isEqualTo(RESERVATION_ID);
            assertThat(response.selectedSeats()).isEmpty();
            assertThat(response.totalAmount()).isEqualTo(0);
            assertThat(response.expiresAt()).isNull();
            assertThat(response.remainingSeconds()).isEqualTo(0L);

            // 공연 정보 확인
            assertThat(response.performance().title()).isEqualTo("임영웅 IM HERO TOUR 2025 - 광주");
            assertThat(response.performance().performanceHallName()).isEqualTo("김대중컨벤션센터");

            // 회차 정보 확인
            assertThat(response.schedule().performanceDate()).isEqualTo(LocalDate.of(2026, 1, 3));
            assertThat(response.schedule().performanceNo()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공: HOLD 상태 - 좌석 정보 포함")
        void getReservationSummary_Success_HoldStatus() {
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

            ReservationSummaryResponse response = reservationService.getReservationSummary(
                    RESERVATION_ID, USER_ID
            );

            assertThat(response).isNotNull();
            assertThat(response.reservationId()).isEqualTo(RESERVATION_ID);
            assertThat(response.selectedSeats()).hasSize(1);
            assertThat(response.totalAmount()).isEqualTo(200_000);
            assertThat(response.expiresAt()).isNotNull();
            assertThat(response.remainingSeconds()).isGreaterThan(0);

            // 좌석 정보 확인
            ReservationSummaryResponse.SelectedSeatInfo seatInfo = response.selectedSeats().get(0);
            assertThat(seatInfo.floor()).isEqualTo(1);
            assertThat(seatInfo.block()).isEqualTo("A");
            assertThat(seatInfo.priceGrade()).isEqualTo("VIP");
            assertThat(seatInfo.price()).isEqualTo(200_000);
        }

        @Test
        @DisplayName("실패: 예매를 찾을 수 없음")
        void getReservationSummary_Fail_ReservationNotFound() {
            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.getReservationSummary(RESERVATION_ID, USER_ID))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 권한 없음 (다른 사용자)")
        void getReservationSummary_Fail_Unauthorized() {
            Long otherUserId = 999L;
            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.getReservationSummary(RESERVATION_ID, otherUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        }

        @Test
        @DisplayName("실패: 스케줄을 찾을 수 없음")
        void getReservationSummary_Fail_ScheduleNotFound() {
            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));
            given(scheduleRepository.findById(SCHEDULE_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.getReservationSummary(RESERVATION_ID, USER_ID))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("updateDeliveryInfo 테스트")
    class UpdateDeliveryInfoTest {

        private UpdateDeliveryInfoRequest request;

        @BeforeEach
        void setUp() {
            request = new UpdateDeliveryInfoRequest(
                    "DELIVERY",
                    new UpdateDeliveryInfoRequest.RecipientInfo(
                            "홍길동",
                            "010-1234-5678",
                            "서울시 강남구 테헤란로 123",
                            "06234"
                    )
            );
        }

        @Test
        @DisplayName("성공: HOLD 상태에서 배송 정보 입력")
        void updateDeliveryInfo_Success_HoldStatus() {
            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));
            given(reservationRepository.save(any(Reservation.class)))
                    .willReturn(reservation);

            UpdateDeliveryInfoResponse response = reservationService.updateDeliveryInfo(
                    RESERVATION_ID, request, USER_ID
            );

            assertThat(response).isNotNull();
            assertThat(response.reservationId()).isEqualTo(RESERVATION_ID);
            assertThat(response.deliveryMethod()).isEqualTo("DELIVERY");
            assertThat(response.expiresAt()).isNotNull();
            assertThat(response.remainingSeconds()).isGreaterThan(0);

            // Reservation 엔티티 메서드 호출 확인
            verify(reservationRepository, times(1)).save(any(Reservation.class));
        }

        @Test
        @DisplayName("성공: 배송 정보 수정 (여러 번 가능)")
        void updateDeliveryInfo_Success_MultipleUpdates() {
            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));
            given(reservationRepository.save(any(Reservation.class)))
                    .willReturn(reservation);

            UpdateDeliveryInfoRequest firstRequest = new UpdateDeliveryInfoRequest(
                    "DELIVERY",
                    new UpdateDeliveryInfoRequest.RecipientInfo(
                            "홍길동",
                            "010-1111-1111",
                            "서울시 강남구",
                            "12345"
                    )
            );

            UpdateDeliveryInfoRequest secondRequest = new UpdateDeliveryInfoRequest(
                    "DELIVERY",
                    new UpdateDeliveryInfoRequest.RecipientInfo(
                            "김철수",
                            "010-2222-2222",
                            "서울시 송파구",
                            "54321"
                    )
            );

            reservationService.updateDeliveryInfo(RESERVATION_ID, firstRequest, USER_ID);
            reservationService.updateDeliveryInfo(RESERVATION_ID, secondRequest, USER_ID);

            verify(reservationRepository, times(2)).save(any(Reservation.class));
        }

        @Test
        @DisplayName("실패: 예매를 찾을 수 없음")
        void updateDeliveryInfo_Fail_ReservationNotFound() {
            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.updateDeliveryInfo(
                    RESERVATION_ID, request, USER_ID
            ))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 권한 없음 (다른 사용자)")
        void updateDeliveryInfo_Fail_Unauthorized() {
            // given
            Long otherUserId = 999L;
            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));

            // when & then
            assertThatThrownBy(() -> reservationService.updateDeliveryInfo(
                    RESERVATION_ID, request, otherUserId
            ))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        }

        @Test
        @DisplayName("실패: PENDING 상태에서 배송 정보 입력 시도")
        void updateDeliveryInfo_Fail_PendingStatus() {
            reservation = Reservation.builder()
                    .userId(USER_ID)
                    .performanceScheduleId(SCHEDULE_ID)
                    .status(ReservationStatus.PENDING)
                    .totalAmount(0)
                    .expiresAt(null)
                    .build();

            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.updateDeliveryInfo(
                    RESERVATION_ID, request, USER_ID
            ))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RESERVATION_STATUS);
        }

        @Test
        @DisplayName("실패: PAID 상태에서 배송 정보 수정 시도")
        void updateDeliveryInfo_Fail_PaidStatus() {
            reservation = Reservation.builder()
                    .userId(USER_ID)
                    .performanceScheduleId(SCHEDULE_ID)
                    .status(ReservationStatus.PAID)
                    .totalAmount(200_000)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .build();

            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.updateDeliveryInfo(
                    RESERVATION_ID, request, USER_ID
            ))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RESERVATION_STATUS);
        }

        @Test
        @DisplayName("실패: CANCELLED 상태에서 배송 정보 입력 시도")
        void updateDeliveryInfo_Fail_CancelledStatus() {
            reservation = Reservation.builder()
                    .userId(USER_ID)
                    .performanceScheduleId(SCHEDULE_ID)
                    .status(ReservationStatus.CANCELLED)
                    .totalAmount(0)
                    .expiresAt(null)
                    .build();

            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.updateDeliveryInfo(
                    RESERVATION_ID, request, USER_ID
            ))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RESERVATION_STATUS);
        }

        @Test
        @DisplayName("실패: 만료된 예매에 배송 정보 입력 시도")
        void updateDeliveryInfo_Fail_ExpiredReservation() {
            reservation = Reservation.builder()
                    .userId(USER_ID)
                    .performanceScheduleId(SCHEDULE_ID)
                    .status(ReservationStatus.HOLD)
                    .totalAmount(200_000)
                    .expiresAt(LocalDateTime.now().minusMinutes(1))  // 이미 만료됨
                    .build();

            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.updateDeliveryInfo(
                    RESERVATION_ID, request, USER_ID
            ))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_EXPIRED);
        }
    }

    @Nested
    @MockitoSettings(strictness = Strictness.LENIENT)
    @DisplayName("cancelReservation 테스트")
    class CancelReservationTest {

        private PerformanceSeat performanceSeat2;
        private PerformanceSeat performanceSeat3;
        private ReservationSeat reservationSeat2;
        private ReservationSeat reservationSeat3;

        @BeforeEach
        void setUp() {
            ValueOperations<String, String> valueOps = mock(ValueOperations.class);
            SetOperations<String, String> setOps = mock(SetOperations.class);

            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(redisTemplate.opsForSet()).willReturn(setOps);
            given(valueOps.increment(anyString())).willReturn(1L);
            willDoNothing().given(valueOps).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
            given(setOps.remove(anyString(), any())).willReturn(1L);

            // 예매가 PAID 상태로 설정
            reservation = Reservation.builder()
                    .userId(USER_ID)
                    .performanceScheduleId(SCHEDULE_ID)
                    .status(ReservationStatus.PAID)
                    .totalAmount(600_000)
                    .expiresAt(LocalDateTime.of(2026, 1, 3, 17, 0))
                    .build();
            ReflectionTestUtils.setField(reservation, "id", RESERVATION_ID);

            // 추가 좌석 생성 (총 3개 좌석)
            performanceSeat2 = PerformanceSeat.create(
                    SCHEDULE_ID, 2L, PRICE_GRADE_ID, 1, "A", 1, 2, 100, 1000
            );
            ReflectionTestUtils.setField(performanceSeat2, "id", 2L);
            performanceSeat2.updateStatus(SeatStatus.SOLD);

            performanceSeat3 = PerformanceSeat.create(
                    SCHEDULE_ID, 3L, PRICE_GRADE_ID, 1, "A", 1, 3, 200, 1000
            );
            ReflectionTestUtils.setField(performanceSeat3, "id", 3L);
            performanceSeat3.updateStatus(SeatStatus.SOLD);

            performanceSeat.updateStatus(SeatStatus.SOLD);

            reservationSeat2 = new ReservationSeat(RESERVATION_ID, 2L, 200_000);
            reservationSeat3 = new ReservationSeat(RESERVATION_ID, 3L, 200_000);
        }

        @Test
        @DisplayName("성공: PAID 상태에서 예매 취소")
        void cancelReservation_Success() {
            // given
            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));
            given(scheduleRepository.findById(SCHEDULE_ID))
                    .willReturn(Optional.of(schedule));
            given(reservationSeatRepository.findByReservationId(RESERVATION_ID))
                    .willReturn(List.of(reservationSeat, reservationSeat2, reservationSeat3));

            List<PerformanceSeat> actualSeats = List.of(performanceSeat, performanceSeat2, performanceSeat3);
            given(performanceSeatRepository.findAllById(anyList()))
                    .willReturn(actualSeats);

            given(reservationRepository.save(any(Reservation.class)))
                    .willReturn(reservation);

            // when
            CancelReservationResponse response = reservationService.cancelReservation(
                    RESERVATION_ID, USER_ID
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.reservationId()).isEqualTo(RESERVATION_ID);
            assertThat(response.status()).isEqualTo("CANCELLED");
            assertThat(response.refundAmount()).isEqualTo(600_000);
            assertThat(response.cancelledSeatCount()).isEqualTo(3);
            assertThat(response.cancelledAt()).isNotNull();

            // 좌석 상태 복구 확인
            ArgumentCaptor<List<PerformanceSeat>> seatCaptor = ArgumentCaptor.forClass(List.class);
            verify(performanceSeatRepository, times(1)).saveAll(seatCaptor.capture());

            List<PerformanceSeat> savedSeats = seatCaptor.getValue();
            assertThat(savedSeats).hasSize(3);
            assertThat(savedSeats.get(0).getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            assertThat(savedSeats.get(0).getHoldUserId()).isNull();

            verify(reservationRepository, times(1)).save(any(Reservation.class));
        }

        @Test
        @DisplayName("실패: 예매를 찾을 수 없음")
        void cancelReservation_Fail_ReservationNotFound() {
            // given
            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reservationService.cancelReservation(RESERVATION_ID, USER_ID))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 권한 없음 (다른 사용자)")
        void cancelReservation_Fail_Unauthorized() {
            // given
            Long otherUserId = 999L;
            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));

            // when & then
            assertThatThrownBy(() -> reservationService.cancelReservation(RESERVATION_ID, otherUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        }

        @Test
        @DisplayName("실패: PENDING 상태에서 취소 시도")
        void cancelReservation_Fail_PendingStatus() {
            // given
            reservation = Reservation.builder()
                    .userId(USER_ID)
                    .performanceScheduleId(SCHEDULE_ID)
                    .status(ReservationStatus.PENDING)
                    .totalAmount(0)
                    .expiresAt(null)
                    .build();
            ReflectionTestUtils.setField(reservation, "id", RESERVATION_ID);

            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));

            // when & then
            assertThatThrownBy(() -> reservationService.cancelReservation(RESERVATION_ID, USER_ID))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RESERVATION_STATUS);
        }

        @Test
        @DisplayName("실패: HOLD 상태에서 취소 시도")
        void cancelReservation_Fail_HoldStatus() {
            // given
            reservation = Reservation.builder()
                    .userId(USER_ID)
                    .performanceScheduleId(SCHEDULE_ID)
                    .status(ReservationStatus.HOLD)
                    .totalAmount(600_000)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();
            ReflectionTestUtils.setField(reservation, "id", RESERVATION_ID);

            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));

            // when & then
            assertThatThrownBy(() -> reservationService.cancelReservation(RESERVATION_ID, USER_ID))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RESERVATION_STATUS);
        }

        @Test
        @DisplayName("실패: CANCELLED 상태에서 재취소 시도")
        void cancelReservation_Fail_AlreadyCancelled() {
            // given
            reservation = Reservation.builder()
                    .userId(USER_ID)
                    .performanceScheduleId(SCHEDULE_ID)
                    .status(ReservationStatus.CANCELLED)
                    .totalAmount(0)
                    .expiresAt(null)
                    .build();
            ReflectionTestUtils.setField(reservation, "id", RESERVATION_ID);

            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));

            // when & then
            assertThatThrownBy(() -> reservationService.cancelReservation(RESERVATION_ID, USER_ID))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_RESERVATION_STATUS);
        }

        @Test
        @DisplayName("실패: 취소 기한 경과 (공연 시작 1시간 이내)")
        void cancelReservation_Fail_CancelDeadlinePassed() {
            // given
            // 공연 시작 30분 전 (취소 불가)
            PerformanceSchedule nearSchedule = new PerformanceSchedule(
                    PERFORMANCE_ID,
                    LocalDate.now(),
                    LocalTime.now().plusMinutes(30),  // 30분 후 시작
                    1,
                    ScheduleStatus.AVAILABLE
            );
            ReflectionTestUtils.setField(nearSchedule, "id", SCHEDULE_ID);

            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));
            given(scheduleRepository.findById(SCHEDULE_ID))
                    .willReturn(Optional.of(nearSchedule));

            // when & then
            assertThatThrownBy(() -> reservationService.cancelReservation(RESERVATION_ID, USER_ID))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANCEL_DEADLINE_PASSED);
        }

        @Test
        @DisplayName("실패: 예매된 좌석이 없음")
        void cancelReservation_Fail_NoSeatsReserved() {
            // given
            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));
            given(scheduleRepository.findById(SCHEDULE_ID))
                    .willReturn(Optional.of(schedule));
            given(reservationSeatRepository.findByReservationId(RESERVATION_ID))
                    .willReturn(List.of());  // 좌석 없음

            // when & then
            assertThatThrownBy(() -> reservationService.cancelReservation(RESERVATION_ID, USER_ID))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_SEATS_RESERVED);
        }

        @Test
        @DisplayName("실패: 스케줄을 찾을 수 없음")
        void cancelReservation_Fail_ScheduleNotFound() {
            // given
            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));
            given(scheduleRepository.findById(SCHEDULE_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reservationService.cancelReservation(RESERVATION_ID, USER_ID))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
        }
    }
}

