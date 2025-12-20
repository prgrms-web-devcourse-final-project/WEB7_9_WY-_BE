package back.kalender.domain.booking.reservation.service;

import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public void cancelReservation(
            Long reservationId,
            Long userId
    ) {
        log.info("[Reservation] 예매 취소 - reservationId: {}, userId: {}", reservationId, userId);

        // TODO: Reservation 조회 및 권한 검증
        // TODO: 결제 완료 여부 검증
        // TODO: 모든 ReservationSeat 삭제
        // TODO: PerformanceSeat 상태 AVAILABLE로 복원
        // TODO: Reservation.cancel() 호출

        log.info("[Reservation] 예매 취소 완료 - reservationId: {}", reservationId);
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
