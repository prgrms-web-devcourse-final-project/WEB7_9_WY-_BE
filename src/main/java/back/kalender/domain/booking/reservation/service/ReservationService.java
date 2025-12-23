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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final ReservationMapper reservationMapper;
    private final BookingSessionService bookingSessionService;

    private static final int CANCEL_DEADLINE_HOURS = 1;

    // 예매 세션 생성
    @Transactional
    public CreateReservationResponse createReservation(
            Long scheduleId,
            CreateReservationRequest request,
            Long userId,
            String bookingSessionId

    ) {
        // 1. BookingSession 검증
        bookingSessionService.validateForSchedule(bookingSessionId, scheduleId);

        // 2. 기존에 진행중인 예매 세션이 있는지 확인 (HOLD/PENDING)
        boolean exists = reservationRepository.existsByUserIdAndPerformanceScheduleIdAndStatusIn(
                userId,
                scheduleId,
                ReservationStatus.activeStatuses()
        );

        if(exists){
            throw new ServiceException(ErrorCode.RESERVATION_ALREADY_EXISTS);
        }

        // 3. Reservation 엔티티 생성 및 저장
        Reservation reservation = Reservation.create(userId, scheduleId);
        Reservation savedReservation = reservationRepository.save(reservation);

        return reservationMapper.toCreateReservationResponse(savedReservation);
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

    // 예매 요약 조회
    public ReservationSummaryResponse getReservationSummary(
            Long reservationId,
            Long userId
    ) {
        // 1. Reservation 조회 및 권한 검증
        Reservation reservation = findAndValidateReservation(reservationId, userId);

        // 2. Performance, Schedule, Hall 정보 조회
        PerformanceSchedule schedule = scheduleRepository.findById(reservation.getPerformanceScheduleId())
                .orElseThrow(() -> new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND));

        Performance performance = performanceRepository.findById(schedule.getPerformanceId())
                .orElseThrow(() -> new ServiceException(ErrorCode.PERFORMANCE_NOT_FOUND));

        PerformanceHall hall = performanceHallRepository.findById(performance.getPerformanceHallId())
                .orElseThrow(() -> new ServiceException(ErrorCode.PERFORMANCE_HALL_NOT_FOUND));

        // 3. 선택된 좌석 정보 조회
        List<ReservationSeat> reservationSeats = reservationSeatRepository.findByReservationId(reservationId);

        List<Long> seatIds = reservationSeats.stream()
                .map(ReservationSeat::getPerformanceSeatId)
                .toList();

        Map<Long, PerformanceSeat> performanceSeatMap = performanceSeatRepository.findAllById(seatIds)
                .stream()
                .collect(Collectors.toMap(PerformanceSeat::getId, s -> s));

        Set<Long> priceGradeIds = performanceSeatMap.values().stream()
                .map(PerformanceSeat::getPriceGradeId)
                .collect(Collectors.toSet());

        Map<Long, PriceGrade> priceGradeMap = priceGradeRepository.findAllById(priceGradeIds)
                .stream()
                .collect(Collectors.toMap(PriceGrade::getId, g -> g));

        return ReservationMapper.toSummaryResponse(
                reservation,
                performance,
                schedule,
                hall,
                reservationSeats,
                performanceSeatMap,
                priceGradeMap,
                LocalDateTime.now()
        );
    }

    // 배송 정보 입력
    @Transactional
    public UpdateDeliveryInfoResponse updateDeliveryInfo(
            Long reservationId,
            UpdateDeliveryInfoRequest request,
            Long userId
    ) {

        // 1. Reservation 조회 및 권한 검증
        Reservation reservation = findAndValidateReservation(reservationId, userId);

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

        Reservation updatedReservation = reservationRepository.save(reservation);

        return reservationMapper.toUpdateDeliveryInfoResponse(updatedReservation);
    }

    // 예매 취소
    public CancelReservationResponse cancelReservation(
            Long reservationId,
            Long userId
    ) {
        // 1. DB 작업 (트랜잭션 커밋)
        CancelReservationData data = cancelReservationInDB(reservationId, userId);

        // 2. Redis 작업 (트랜잭션 외부 - DB 커밋 완료 후)
        try {
            updateRedisAfterCancel(data.scheduleId(), data.seatIds());
        } catch (Exception e) {
            /*
             * Redis 실패 처리:
             * - DB는 이미 커밋됨 (정합성 유지)
             * - Redis는 캐시 역할이므로 TTL로 자동 정리
             * - 폴링 API는 Redis 이벤트 기반이지만, Redis 실패 시에도
             *   다음 HOLD/RELEASE에서 Redis가 정리되므로 최종 일관성 유지
             */
            log.error("[Reservation] Redis 업데이트 실패 (DB는 이미 커밋됨) - reservationId={}",
                    reservationId, e);
        }

        // 3. Mapper로 응답 생성
        return reservationMapper.toCancelReservationResponse(
                data.reservation(),
                data.seatIds().size()
        );
    }

    // 예매 취소 - DB 작업
    @Transactional
    protected CancelReservationData cancelReservationInDB(Long reservationId, Long userId) {
        // 1. Reservation 조회 및 권한 검증
        Reservation reservation = findAndValidateReservation(reservationId, userId);

        // 2. 취소 가능 상태 검증 (PAID만 가능)
        if (reservation.getStatus() != ReservationStatus.PAID) {
            throw new ServiceException(ErrorCode.INVALID_RESERVATION_STATUS);
        }

        // 3. 취소 가능 기한 확인 (공연 시작 1시간 전까지)
        PerformanceSchedule schedule = scheduleRepository.findById(reservation.getPerformanceScheduleId())
                .orElseThrow(() -> new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND));

        LocalDateTime cancelDeadline = LocalDateTime.of(
                schedule.getPerformanceDate(),
                schedule.getStartTime()
        ).minusHours(CANCEL_DEADLINE_HOURS);

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
            seat.clearHoldInfo();
        }
        performanceSeatRepository.saveAll(seats);

        // 6. 예매 상태 변경 (PAID → CANCELLED)
        reservation.cancel();
        Reservation cancelledReservation = reservationRepository.save(reservation);

        log.info("[Reservation] 예매 취소 DB 완료 - reservationId={}, userId={}, seatCount={}",
                reservationId, userId, seatIds.size());

        return new CancelReservationData(
                cancelledReservation,
                schedule.getId(),
                seatIds
        );
    }

    // 예매 취소 후 Redis 업데이트
    protected void updateRedisAfterCancel(Long scheduleId, List<Long> seatIds) {
        // 1. Redis SOLD set에서 제거
        String soldSetKey = String.format("seat:sold:%d", scheduleId);
        for (Long seatId : seatIds) {
            redisTemplate.opsForSet().remove(soldSetKey, seatId.toString());
        }

        // 2. 좌석 변경 이벤트 발행 (폴링용)
        for (Long seatId : seatIds) {
            try {
                recordSeatChangeEvent(scheduleId, seatId, SeatStatus.AVAILABLE, null);
            } catch (Exception e) {
                // 이벤트 발행 실패는 로그만 (폴링에 영향 있지만 치명적이지 않음)
                log.warn("[Reservation] 변경 이벤트 발행 실패 - seatId={}", seatId, e);
            }
        }

        log.debug("[Reservation] Redis 업데이트 완료 - scheduleId={}, seatCount={}",
                scheduleId, seatIds.size());
    }

    // 예매 취소 DB 작업 결과 DTO
    protected record CancelReservationData(
            Reservation reservation,
            Long scheduleId,
            List<Long> seatIds
    ) {}

    // 좌석 변경 이벤트 발행 (폴링 API용)
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

    // 좌석 변경 내역 조회 (폴링)
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

    // 예매내역 조회
    public MyReservationListResponse getMyReservations(Long userId, Pageable pageable) {
        // 1. 완료된 예매만 조회
        List<ReservationStatus> completedStatuses = List.of(
                ReservationStatus.PAID,
                ReservationStatus.CANCELLED
        );

        Page<Reservation> reservationPage = reservationRepository
                .findByUserIdAndStatusInOrderByCreatedAtDesc(userId, completedStatuses, pageable);

        if (reservationPage.isEmpty()) {
            return new MyReservationListResponse(
                    List.of(),
                    reservationPage.getNumber(),
                    reservationPage.getTotalPages(),
                    reservationPage.getTotalElements()
            );
        }

        // 2. Schedule ID 추출 및 조회
        Set<Long> scheduleIds = reservationPage.getContent().stream()
                .map(Reservation::getPerformanceScheduleId)
                .collect(Collectors.toSet());

        Map<Long, PerformanceSchedule> scheduleMap = scheduleRepository.findAllById(scheduleIds)
                .stream()
                .collect(Collectors.toMap(PerformanceSchedule::getId, s -> s));

        // 3. Performance ID 추출 및 조회
        Set<Long> performanceIds = scheduleMap.values().stream()
                .map(PerformanceSchedule::getPerformanceId)
                .collect(Collectors.toSet());

        Map<Long, Performance> performanceMap = performanceRepository.findAllById(performanceIds)
                .stream()
                .collect(Collectors.toMap(Performance::getId, p -> p));

        // 4. Hall ID 추출 및 조회
        Set<Long> hallIds = performanceMap.values().stream()
                .map(Performance::getPerformanceHallId)
                .collect(Collectors.toSet());

        Map<Long, PerformanceHall> hallMap = performanceHallRepository.findAllById(hallIds)
                .stream()
                .collect(Collectors.toMap(PerformanceHall::getId, h -> h));

        // 5. 예매별 좌석 수 조회
        List<Long> reservationIds = reservationPage.getContent().stream()
                .map(Reservation::getId)
                .toList();

        List<Object[]> seatCounts = reservationSeatRepository.countByReservationIds(reservationIds);
        Map<Long, Long> seatCountMap = seatCounts.stream()
                .collect(Collectors.toMap(arr -> (Long) arr[0], arr -> (Long) arr[1]));

        List<MyReservationListResponse.ReservationItem> items = reservationPage.getContent().stream()
                .map(reservation -> {
                    PerformanceSchedule schedule = scheduleMap.get(reservation.getPerformanceScheduleId());
                    Performance performance = performanceMap.get(schedule.getPerformanceId());
                    PerformanceHall hall = hallMap.get(performance.getPerformanceHallId());
                    int seatCount = seatCountMap.getOrDefault(reservation.getId(), 0L).intValue();

                    return reservationMapper.toMyReservationItem(
                            reservation, schedule, performance, hall, seatCount
                    );
                })
                .toList();

        return new MyReservationListResponse(
                items,
                reservationPage.getNumber(),
                reservationPage.getTotalPages(),
                reservationPage.getTotalElements()
        );
    }

    // 예매 상세 조회
    public ReservationDetailResponse getReservationDetail(Long reservationId, Long userId) {
        // 1. Reservation 조회 및 권한 검증
        Reservation reservation = findAndValidateReservation(reservationId, userId);

        // 2. Schedule 조회
        PerformanceSchedule schedule = scheduleRepository.findById(reservation.getPerformanceScheduleId())
                .orElseThrow(() -> new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND));

        // 3. Performance 조회
        Performance performance = performanceRepository.findById(schedule.getPerformanceId())
                .orElseThrow(() -> new ServiceException(ErrorCode.PERFORMANCE_NOT_FOUND));

        // 4. Hall 조회
        PerformanceHall hall = performanceHallRepository.findById(performance.getPerformanceHallId())
                .orElseThrow(() -> new ServiceException(ErrorCode.PERFORMANCE_HALL_NOT_FOUND));

        // 5. 좌석 정보 조회
        List<ReservationSeat> reservationSeats = reservationSeatRepository
                .findByReservationId(reservationId);

        List<Long> seatIds = reservationSeats.stream()
                .map(ReservationSeat::getPerformanceSeatId)
                .toList();

        Map<Long, PerformanceSeat> seatMap = performanceSeatRepository.findAllById(seatIds)
                .stream()
                .collect(Collectors.toMap(PerformanceSeat::getId, s -> s));

        Set<Long> priceGradeIds = seatMap.values().stream()
                .map(PerformanceSeat::getPriceGradeId)
                .collect(Collectors.toSet());

        Map<Long, PriceGrade> priceGradeMap = priceGradeRepository.findAllById(priceGradeIds)
                .stream()
                .collect(Collectors.toMap(PriceGrade::getId, g -> g));

        return reservationMapper.toReservationDetailResponse(
                reservation,
                schedule,
                performance,
                hall,
                reservationSeats,
                seatMap,
                priceGradeMap
        );
    }

    // 예매 만료 처리 - 스케줄러에서 호출
    @Transactional
    public void expireReservationAndReleaseSeats(Long reservationId, Long userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ServiceException(ErrorCode.RESERVATION_NOT_FOUND));

        // userId가 null이 아닐 때만 권한 체크
        if (userId != null && !reservation.isOwnedBy(userId)) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED);
        }
        Long scheduleId = reservation.getPerformanceScheduleId();

        // 2. HOLD 좌석 조회
        List<ReservationSeat> reservationSeats =
                reservationSeatRepository.findByReservationId(reservationId);

        if (reservationSeats.isEmpty()) {
            log.warn("[Reservation] 해제할 좌석 없음 - reservationId={}", reservationId);
            reservation.expire();
            reservationRepository.save(reservation);
            return;
        }

        List<Long> seatIds = reservationSeats.stream()
                .map(ReservationSeat::getPerformanceSeatId)
                .toList();

        // 3. DB 좌석 상태 복구
        List<PerformanceSeat> seats = performanceSeatRepository.findAllById(seatIds);
        for (PerformanceSeat seat : seats) {
            if (seat.getStatus() == SeatStatus.HOLD) {
                seat.updateStatus(SeatStatus.AVAILABLE);
                seat.clearHoldInfo();
            }
        }
        performanceSeatRepository.saveAll(seats);

        // 4. Redis HOLD owner 키 삭제
        for (Long seatId : seatIds) {
            String holdOwnerKey = String.format(
                    "seat:hold:owner:%d:%d",
                    scheduleId,
                    seatId
            );
            redisTemplate.delete(holdOwnerKey);

            // 폴링 이벤트 발행
            recordSeatChangeEvent(scheduleId, seatId, SeatStatus.AVAILABLE, null);
        }

        // 5. Reservation 만료
        reservation.expire();
        reservationRepository.save(reservation);

        // 6. ReservationSeat 삭제 (선택)
        // reservationSeatRepository.deleteByReservationId(reservationId);

        log.info("[Reservation] 예매 만료 처리 완료 - reservationId={}, seatCount={}",
                reservationId, seatIds.size());
    }

    private Reservation findAndValidateReservation(Long reservationId, Long userId) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ServiceException(ErrorCode.RESERVATION_NOT_FOUND));
        if (!r.isOwnedBy(userId)) throw new ServiceException(ErrorCode.UNAUTHORIZED);
        return r;
    }

    /**
     * TODO: 결제 완료 시 좌석을 SOLD로 표시 (C파트에서 호출)
     */
    @Transactional
    public void markSeatsAsSold(Long scheduleId, Long reservationId) {
        // 1. ReservationSeat 조회
        List<ReservationSeat> seats = reservationSeatRepository
                .findByReservationId(reservationId);

        List<Long> seatIds = seats.stream()
                .map(ReservationSeat::getPerformanceSeatId)
                .toList();

        // 2. DB: HOLD → SOLD
        List<PerformanceSeat> performanceSeats =
                performanceSeatRepository.findAllById(seatIds);

        for (PerformanceSeat seat : performanceSeats) {
            seat.updateStatus(SeatStatus.SOLD);
            seat.clearHoldInfo(); // holdUserId, holdExpiredAt 제거
        }
        performanceSeatRepository.saveAll(performanceSeats);

        // 3. Redis: HOLD owner 삭제 + SOLD set 추가
        String soldSetKey = String.format("seat:sold:%d", scheduleId);

        for (Long seatId : seatIds) {
            // HOLD owner 키 삭제
            String holdOwnerKey = String.format(
                    "seat:hold:owner:%d:%d",
                    scheduleId,
                    seatId
            );
            redisTemplate.delete(holdOwnerKey);

            // SOLD set에 추가
            redisTemplate.opsForSet().add(
                    soldSetKey,
                    seatId.toString()
            );

            // 변경 이벤트 발행
            recordSeatChangeEvent(scheduleId, seatId, SeatStatus.SOLD, null);
        }

        log.info("[Reservation] 좌석 SOLD 처리 완료 - reservationId={}, seatCount={}",
                reservationId, seatIds.size());
    }
}
