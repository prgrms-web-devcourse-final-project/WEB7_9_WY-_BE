package back.kalender.domain.booking.seatHold.service;

import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.booking.reservation.dto.request.HoldSeatsRequest;
import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservation.entity.ReservationStatus;
import back.kalender.domain.booking.reservation.repository.ReservationRepository;
import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
import back.kalender.domain.booking.reservationSeat.repository.ReservationSeatRepository;
import back.kalender.domain.booking.seatHold.event.SeatHoldCompletedEvent;
import back.kalender.domain.booking.seatHold.event.SeatReleaseCompletedEvent;
import back.kalender.domain.booking.seatHold.exception.SeatHoldConflictException;
import back.kalender.domain.booking.seatHold.repository.SeatHoldLogRepository;
import back.kalender.domain.performance.priceGrade.entity.PriceGrade;
import back.kalender.domain.performance.priceGrade.repository.PriceGradeRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatHoldService 단위 테스트")
class SeatHoldServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PerformanceSeatRepository performanceSeatRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationSeatRepository reservationSeatRepository;

    @Mock
    private SeatHoldLogRepository seatHoldLogRepository;

    @Mock
    private PriceGradeRepository priceGradeRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SeatHoldService seatHoldService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private RLock lock;

    private Reservation reservation;
    private PerformanceSeat seat;
    private PriceGrade priceGrade;
    private Long scheduleId = 1L;
    private Long seatId = 101L;
    private Long userId = 1000L;
    private Long reservationId = 1L;
    private Long priceGradeId = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);

        reservation = Reservation.create(userId, scheduleId, "test-session");
        ReflectionTestUtils.setField(reservation, "id", reservationId);
        reservation.updateStatus(ReservationStatus.PENDING);

        seat = PerformanceSeat.create(
                scheduleId, 1L, priceGradeId,
                1, "A", null, 1, 1, 10, 10
        );
        ReflectionTestUtils.setField(seat, "id", seatId);

        priceGrade = PriceGrade.builder()
                .performanceId(1L)
                .gradeName("VIP")
                .price(150000)
                .build();
        ReflectionTestUtils.setField(priceGrade, "id", priceGradeId);
    }

    @Test
    @DisplayName("좌석 HOLD 성공 - 이벤트 발행 확인")
    void holdSeats_Success() throws InterruptedException {
        HoldSeatsRequest request = new HoldSeatsRequest(List.of(seatId));

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        when(performanceSeatRepository.findByIdAndScheduleId(seatId, scheduleId))
                .thenReturn(Optional.of(seat));

        when(setOperations.isMember(anyString(), anyString())).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(priceGradeRepository.findById(priceGradeId)).thenReturn(Optional.of(priceGrade));
        when(reservationSeatRepository.findByReservationId(reservationId)).thenReturn(List.of());

        seatHoldService.holdSeats(reservationId, request, userId);

        ArgumentCaptor<SeatHoldCompletedEvent> eventCaptor =
                ArgumentCaptor.forClass(SeatHoldCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        SeatHoldCompletedEvent event = eventCaptor.getValue();
        assertThat(event.getScheduleId()).isEqualTo(scheduleId);
        assertThat(event.getSeatId()).isEqualTo(seatId);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getStatus()).isEqualTo(SeatStatus.HOLD);

        verify(performanceSeatRepository).save(seat);
        verify(reservationSeatRepository).save(any(ReservationSeat.class));
        verify(seatHoldLogRepository).save(any());
        verify(lock).unlock();
    }

    @Test
    @DisplayName("이미 SOLD된 좌석은 HOLD 불가")
    void holdSeats_AlreadySold_ThrowsException() throws InterruptedException {
        HoldSeatsRequest request = new HoldSeatsRequest(List.of(seatId));
        seat.updateStatus(SeatStatus.SOLD);

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(performanceSeatRepository.findByIdAndScheduleId(seatId, scheduleId))
                .thenReturn(Optional.of(seat));
        when(setOperations.isMember(anyString(), eq(seatId.toString()))).thenReturn(true);

        assertThatThrownBy(() ->
                seatHoldService.holdSeats(reservationId, request, userId)
        ).isInstanceOf(SeatHoldConflictException.class);

        verify(eventPublisher, never()).publishEvent(any(SeatHoldCompletedEvent.class));
        verify(lock).unlock();
    }

    @Test
    @DisplayName("권한 없는 사용자의 HOLD 시도 차단")
    void holdSeats_Unauthorized_ThrowsException() {
        // given
        Long wrongUserId = 9999L;
        HoldSeatsRequest request = new HoldSeatsRequest(List.of(seatId));

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        // when & then
        // 🔥 ErrorCode의 code 값으로 검증
        assertThatThrownBy(() ->
                seatHoldService.holdSeats(reservationId, request, wrongUserId)
        ).isInstanceOf(ServiceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(eventPublisher, never()).publishEvent(any());
        verify(redissonClient, never()).getLock(anyString());
    }

    @Test
    @DisplayName("다른 사용자가 HOLD 중인 좌석은 HOLD 불가")
    void holdSeats_AlreadyHeldByOther_ThrowsException() throws InterruptedException {
        HoldSeatsRequest request = new HoldSeatsRequest(List.of(seatId));

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(performanceSeatRepository.findByIdAndScheduleId(seatId, scheduleId))
                .thenReturn(Optional.of(seat));
        when(setOperations.isMember(anyString(), anyString())).thenReturn(false);
        when(valueOperations.get("seat:hold:owner:" + scheduleId + ":" + seatId))
                .thenReturn("9999");

        assertThatThrownBy(() ->
                seatHoldService.holdSeats(reservationId, request, userId)
        ).isInstanceOf(SeatHoldConflictException.class);

        verify(eventPublisher, never()).publishEvent(any(SeatHoldCompletedEvent.class));
        verify(lock).unlock();
    }

    @Test
    @DisplayName("락 획득 실패 시 HOLD 실패")
    void holdSeats_LockAcquisitionFailed_ThrowsException() throws InterruptedException {
        HoldSeatsRequest request = new HoldSeatsRequest(List.of(seatId));

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(false);

        assertThatThrownBy(() ->
                seatHoldService.holdSeats(reservationId, request, userId)
        ).isInstanceOf(SeatHoldConflictException.class);

        verify(eventPublisher, never()).publishEvent(any());
        verify(lock, never()).unlock();
    }

    @Test
    @DisplayName("존재하지 않는 좌석 HOLD 시도")
    void holdSeats_SeatNotFound_ThrowsException() throws InterruptedException {
        HoldSeatsRequest request = new HoldSeatsRequest(List.of(seatId));

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(performanceSeatRepository.findByIdAndScheduleId(seatId, scheduleId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                seatHoldService.holdSeats(reservationId, request, userId)
        ).isInstanceOf(ServiceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERFORMANCE_SEAT_NOT_FOUND);

        verify(eventPublisher, never()).publishEvent(any());
        verify(lock).unlock();
    }

    @Test
    @DisplayName("이미 PAID 상태인 예매는 HOLD 불가")
    void holdSeats_AlreadyPaid_ThrowsException() {
        HoldSeatsRequest request = new HoldSeatsRequest(List.of(seatId));
        reservation.updateStatus(ReservationStatus.PAID);

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() ->
                seatHoldService.holdSeats(reservationId, request, userId)
        ).isInstanceOf(ServiceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALREADY_PAID_RESERVATION);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("여러 좌석 중 일부 실패 시 전체 롤백")
    void holdSeats_PartialFailure_RollbackAll() throws InterruptedException {
        Long seatId2 = 102L;

        PerformanceSeat seat2 = PerformanceSeat.create(
                scheduleId, 2L, priceGradeId,
                1, "A", null, 1, 2, 20, 10
        );
        ReflectionTestUtils.setField(seat2, "id", seatId2);
        seat2.updateStatus(SeatStatus.SOLD);

        HoldSeatsRequest request = new HoldSeatsRequest(List.of(seatId, seatId2));

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        // 첫 번째 좌석: 성공
        when(performanceSeatRepository.findByIdAndScheduleId(seatId, scheduleId))
                .thenReturn(Optional.of(seat));
        when(setOperations.isMember(anyString(), eq(seatId.toString()))).thenReturn(false);
        when(valueOperations.get("seat:hold:owner:" + scheduleId + ":" + seatId)).thenReturn(null);

        // 두 번째 좌석: SOLD
        when(performanceSeatRepository.findByIdAndScheduleId(seatId2, scheduleId))
                .thenReturn(Optional.of(seat2));
        when(setOperations.isMember(anyString(), eq(seatId2.toString()))).thenReturn(true);

        when(priceGradeRepository.findById(priceGradeId)).thenReturn(Optional.of(priceGrade));

        assertThatThrownBy(() ->
                seatHoldService.holdSeats(reservationId, request, userId)
        ).isInstanceOf(SeatHoldConflictException.class);

        // 롤백 검증:
        // 1. 첫 번째 좌석 HOLD 이벤트 발행 (holdSingleSeatInDB)
        // 2. 두 번째 좌석 실패로 롤백
        // 3. 첫 번째 좌석 RELEASE 이벤트 발행 (rollbackHeldSeats)

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

        List<Object> events = eventCaptor.getAllValues();

        // 첫 번째 이벤트: HOLD
        assertThat(events.get(0)).isInstanceOf(SeatHoldCompletedEvent.class);
        SeatHoldCompletedEvent holdEvent = (SeatHoldCompletedEvent) events.get(0);
        assertThat(holdEvent.getSeatId()).isEqualTo(seatId);
        assertThat(holdEvent.getStatus()).isEqualTo(SeatStatus.HOLD);

        // 두 번째 이벤트: RELEASE (롤백)
        assertThat(events.get(1)).isInstanceOf(SeatReleaseCompletedEvent.class);
        SeatReleaseCompletedEvent releaseEvent = (SeatReleaseCompletedEvent) events.get(1);
        assertThat(releaseEvent.getSeatId()).isEqualTo(seatId);
        assertThat(releaseEvent.getStatus()).isEqualTo(SeatStatus.AVAILABLE);

        verify(performanceSeatRepository, atLeastOnce()).save(seat);
        verify(lock, atLeast(2)).unlock();
    }
}