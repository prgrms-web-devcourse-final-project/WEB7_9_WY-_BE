package back.kalender.domain.booking.reservation.service;

import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.booking.reservation.dto.request.CreateReservationRequest;
import back.kalender.domain.booking.reservation.dto.request.HoldSeatsRequest;
import back.kalender.domain.booking.reservation.dto.request.ReleaseSeatsRequest;
import back.kalender.domain.booking.reservation.dto.request.UpdateDeliveryInfoRequest;
import back.kalender.domain.booking.reservation.dto.response.*;
import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservation.entity.ReservationStatus;
import back.kalender.domain.booking.reservation.repository.ReservationRepository;
import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
import back.kalender.domain.booking.reservationSeat.repository.ReservationSeatRepository;
import back.kalender.domain.booking.seatHold.service.SeatHoldService;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * - ReservationService: 예매 세션 생성, 배송 정보, 취소 등 전반적인 예매 관리
 * - SeatHoldService: 좌석 HOLD/RELEASE 처리 (분산 락, Redis 캐싱)
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final SeatHoldService seatHoldService;
    private final PerformanceScheduleRepository scheduleRepository;
    private final PerformanceHallRepository performanceHallRepository;
    private final PerformanceRepository performanceRepository;
    private final PriceGradeRepository priceGradeRepository;
    private final PerformanceSeatRepository performanceSeatRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // 예매 세션 생성
    @Transactional
    public CreateReservationResponse createReservation(
            Long scheduleId,
            CreateReservationRequest request,
            Long userId
    ) {
        // 1. Schedule 조회 및 검증
         PerformanceSchedule schedule = scheduleRepository.findById(scheduleId)
             .orElseThrow(() -> new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND));

         if(schedule.getStatus() != ScheduleStatus.AVAILABLE) {
             throw new ServiceException(ErrorCode.SCHEDULE_NOT_AVAILABLE);
         }
        // TODO: 대기열 토큰 검증
        // TODO: Schedule 상태 검증 (예매 가능한 상태인지)

        // 2. Reservation 엔티티 생성 및 저장
        Reservation reservation = Reservation.builder()
                .userId(userId)
                .performanceScheduleId(scheduleId)
                .status(ReservationStatus.PENDING)
                .totalAmount(0)  // 좌석 선택 전이므로 0원
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        return new CreateReservationResponse(
                savedReservation.getId(),
                savedReservation.getStatus().name(),
                null,
                0L
        );
    }

    // 좌석 홀드
    @Transactional
    public HoldSeatsResponse holdSeats(
            Long reservationId,
            HoldSeatsRequest request,
            Long userId
    ) {
        return seatHoldService.holdSeats(reservationId, request, userId);
    }

    // 좌석 홀드 해제
    @Transactional
    public ReleaseSeatsResponse releaseSeats(
            Long reservationId,
            ReleaseSeatsRequest request,
            Long userId
    ) {
        return seatHoldService.releaseSeats(reservationId, request, userId);
    }

    public ReservationSummaryResponse getReservationSummary(
            Long reservationId,
            Long userId
    ) {
        // 1. Reservation 조회 및 권한 검증
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ServiceException(ErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.isOwnedBy(userId)) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED);
        }

        // 2. Performance, Schedule, Hall 정보 조회
        PerformanceSchedule schedule = scheduleRepository.findById(reservation.getPerformanceScheduleId())
                .orElseThrow(() -> new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND));

        Performance performance = performanceRepository.findById(schedule.getPerformanceId())
                .orElseThrow(() -> new ServiceException(ErrorCode.PERFORMANCE_NOT_FOUND));

        PerformanceHall hall = performanceHallRepository.findById(performance.getPerformanceHallId())
                .orElseThrow(() -> new ServiceException(ErrorCode.PERFORMANCE_HALL_NOT_FOUND));

        // 3. 선택된 좌석 정보 조회
        List<ReservationSeat> reservationSeats = reservationSeatRepository.findByReservationId(reservationId);

        // 좌석 없으면 빈 리스트
        List<ReservationSummaryResponse.SelectedSeatInfo> selectedSeats = new ArrayList<>();

        if (!reservationSeats.isEmpty()) {
            // PerformanceSeat 정보 조회
            List<Long> seatIds = reservationSeats.stream()
                    .map(ReservationSeat::getPerformanceSeatId)
                    .toList();

            Map<Long, PerformanceSeat> seatMap = performanceSeatRepository.findAllById(seatIds)
                    .stream()
                    .collect(Collectors.toMap(PerformanceSeat::getId, seat -> seat));

            // PriceGrade 정보 조회
            Set<Long> priceGradeIds = seatMap.values().stream()
                    .map(PerformanceSeat::getPriceGradeId)
                    .collect(Collectors.toSet());

            Map<Long, PriceGrade> priceGradeMap = priceGradeRepository.findAllById(priceGradeIds)
                    .stream()
                    .collect(Collectors.toMap(PriceGrade::getId, grade -> grade));

            // SelectedSeatInfo 생성
            for (ReservationSeat reservationSeat : reservationSeats) {
                PerformanceSeat seat = seatMap.get(reservationSeat.getPerformanceSeatId());
                PriceGrade priceGrade = priceGradeMap.get(seat.getPriceGradeId());

                selectedSeats.add(new ReservationSummaryResponse.SelectedSeatInfo(
                        seat.getId(),
                        seat.getFloor(),
                        seat.getBlock(),
                        seat.getRowNumber(),
                        seat.getSeatNumber(),
                        priceGrade.getGradeName(),
                        reservationSeat.getPrice()
                ));
            }
        }

        // 4. 홀드 만료까지 남은 시간 계산
        Long remainingSeconds = 0L;
        if (reservation.getExpiresAt() != null) {
            long seconds = ChronoUnit.SECONDS.between(
                    LocalDateTime.now(),
                    reservation.getExpiresAt()
            );
            remainingSeconds = Math.max(0, seconds);
        }

        // 5 취소 가능 기한 계산 (공연 시작 1시간 전)
        LocalDateTime cancelDeadline = LocalDateTime.of(
                schedule.getPerformanceDate(),
                schedule.getStartTime()
        ).minusHours(1);

        return new ReservationSummaryResponse(
                reservation.getId(),
                new ReservationSummaryResponse.PerformanceInfo(
                        performance.getId(),
                        performance.getTitle(),
                        performance.getPosterImageUrl(),
                        hall.getName()
                ),
                new ReservationSummaryResponse.ScheduleInfo(
                        schedule.getId(),
                        schedule.getPerformanceDate(),
                        schedule.getStartTime(),
                        schedule.getPerformanceNo()
                ),
                selectedSeats,
                reservation.getTotalAmount(),
                reservation.getExpiresAt(),
                remainingSeconds,
                cancelDeadline
        );
    }

    @Transactional
    public UpdateDeliveryInfoResponse updateDeliveryInfo(
            Long reservationId,
            UpdateDeliveryInfoRequest request,
            Long userId
    ) {

        // 1. Reservation 조회 및 권한 검증
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ServiceException(ErrorCode.RESERVATION_NOT_FOUND));

        // 소유자 확인
        if (!reservation.isOwnedBy(userId)) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED);
        }

        // 2. 상태 검증 (HOLD상태만)
        if (reservation.getStatus() != ReservationStatus.HOLD){
            throw new ServiceException(ErrorCode.INVALID_RESERVATION_STATUS);
        }
        // 만료 확인
        if (reservation.isExpired()) {
            throw new ServiceException(ErrorCode.RESERVATION_EXPIRED);
        }

        // 3. 배송 정보 업데이트
        reservation.updateDeliveryInfo(
                request.deliveryMethod(),
                request.recipient().name(),
                request.recipient().phone(),
                request.recipient().address(),
                request.recipient().zipCode()
        );

        reservationRepository.save(reservation);

        log.info("[Reservation] 배송 정보 입력 완료 - reservationId={}, method={}",
                reservationId, request.deliveryMethod());

        // 4. 남은 시간 계산
        Long remainingSeconds = 0L;
        if (reservation.getExpiresAt() != null) {
            long seconds = ChronoUnit.SECONDS.between(
                    LocalDateTime.now(),
                    reservation.getExpiresAt()
            );
            remainingSeconds = Math.max(0, seconds);
        }

        return new UpdateDeliveryInfoResponse(
                reservation.getId(),
                reservation.getDeliveryMethod(),
                LocalDateTime.now(),
                reservation.getExpiresAt(),
                remainingSeconds
        );
    }

    @Transactional
    public CancelReservationResponse cancelReservation(
            Long reservationId,
            Long userId
    ) {
        // 1. Reservation 조회 및 권한 검증
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ServiceException(ErrorCode.RESERVATION_NOT_FOUND));

        // 소유자 확인
        if (!reservation.isOwnedBy(userId)) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED);
        }

        // 2. 취소 가능 상태 검증
        if (reservation.getStatus() != ReservationStatus.PAID) {
            throw new ServiceException(ErrorCode.INVALID_RESERVATION_STATUS);
        }

        // 3. 취소 가능 기한 확인 (공연 시작 1시간 전까지)
        PerformanceSchedule schedule = scheduleRepository.findById(reservation.getPerformanceScheduleId())
                .orElseThrow(() -> new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND));

        LocalDateTime performanceStartTime = LocalDateTime.of(
                schedule.getPerformanceDate(),
                schedule.getStartTime()
        );
        LocalDateTime cancelDeadline = performanceStartTime.minusHours(1);

        if (LocalDateTime.now().isAfter(cancelDeadline)) {
            throw new ServiceException(ErrorCode.CANCEL_DEADLINE_PASSED);
        }

        // 4. 예매된 좌석 조회
        List<ReservationSeat> reservationSeats = reservationSeatRepository
                .findByReservationId(reservationId);

        if (reservationSeats.isEmpty()) {
            throw new ServiceException(ErrorCode.NO_SEATS_RESERVED);
        }

        List<Long> seatIds = reservationSeats.stream()
                .map(ReservationSeat::getPerformanceSeatId)
                .toList();


        // 5. 좌석 상태 복구 (SOLD → AVAILABLE)
        List<PerformanceSeat> seats = performanceSeatRepository.findAllById(seatIds);

        for (PerformanceSeat seat : seats) {
            seat.updateStatus(SeatStatus.AVAILABLE);
            seat.clearHoldInfo();  // holdUserId, holdExpiresAt 초기화
        }
        performanceSeatRepository.saveAll(seats);  // ← 이 줄 추가!


        // 6. Redis SOLD set에서 제거
        String soldSetKey = String.format("seat:sold:%d", schedule.getId());
        for (Long seatId : seatIds) {
            redisTemplate.opsForSet().remove(soldSetKey, seatId.toString());
        }

        // 7. 좌석 변경 이벤트 발행 (폴링용)
        for (Long seatId : seatIds) {
            recordSeatChangeEvent(
                    schedule.getId(),
                    seatId,
                    SeatStatus.AVAILABLE,
                    null  // userId = null (예매 취소)
            );
        }

        // 8. 예매 상태 변경 (PAID → CANCELLED)
        reservation.cancel();
        reservationRepository.save(reservation);

        log.info("[Reservation] 예매 취소 완료 - reservationId={}, userId={}, seatCount={}",
                reservationId, userId, seatIds.size());

        // 9. 응답 생성
        return new CancelReservationResponse(
                reservationId,
                ReservationStatus.CANCELLED.name(),
                reservation.getTotalAmount(),  // 환불 예정 금액
                LocalDateTime.now(),  // cancelledAt
                seatIds.size()
        );
    }

    /**
     * 좌석 변경 이벤트 발행 (폴링 API용)
     */
    private void recordSeatChangeEvent(
            Long scheduleId,
            Long seatId,
            SeatStatus status,
            Long userId
    ) {
        // 버전 증가
        String versionKey = String.format("seat:version:%d", scheduleId);
        Long version = redisTemplate.opsForValue().increment(versionKey);

        // 변경 이벤트 저장 (TTL 60초)
        String changeKey = String.format("seat:changes:%d:%d", scheduleId, version);
        Map<String, Object> changeEvent = Map.of(
                "seatId", seatId,
                "status", status.name(),
                "userId", userId != null ? userId : "null",
                "version", version,
                "timestamp", LocalDateTime.now().toString()
        );

        try {
            String eventJson = objectMapper.writeValueAsString(changeEvent);
            redisTemplate.opsForValue().set(changeKey, eventJson, 60, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[SeatChange] 이벤트 발행 실패 - scheduleId={}, seatId={}", scheduleId, seatId, e);
        }
    }

    public SeatChangesResponse getSeatChanges(Long scheduleId, Long sinceVersion) {
        List<Map<String, Object>> changes = seatHoldService.getSeatChanges(
                scheduleId,
                sinceVersion
        );

        // Map을 DTO로 변환
        List<SeatChangesResponse.SeatChangeEvent> events = changes.stream()
                .map(this::mapToSeatChangeEvent)
                .toList();

        // 현재 최신 버전 조회
        Long currentVersion = events.isEmpty() ?
                sinceVersion :
                events.get(events.size() - 1).version();

        return new SeatChangesResponse(currentVersion, events);
    }

    private SeatChangesResponse.SeatChangeEvent mapToSeatChangeEvent(Map<String, Object> map) {
        return new SeatChangesResponse.SeatChangeEvent(
                ((Number) map.get("seatId")).longValue(),
                (String) map.get("status"),
                ((Number) map.get("userId")).longValue(),
                ((Number) map.get("version")).longValue(),
                (String) map.get("timestamp")
        );
    }
}
