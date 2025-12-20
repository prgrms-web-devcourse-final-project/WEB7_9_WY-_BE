package back.kalender.domain.booking.reservation.service;

import back.kalender.domain.booking.reservation.dto.request.CreateReservationRequest;
import back.kalender.domain.booking.reservation.dto.request.HoldSeatsRequest;
import back.kalender.domain.booking.reservation.dto.request.ReleaseSeatsRequest;
import back.kalender.domain.booking.reservation.dto.request.UpdateDeliveryInfoRequest;
import back.kalender.domain.booking.reservation.dto.response.*;
import back.kalender.domain.booking.reservation.repository.ReservationRepository;
import back.kalender.domain.booking.reservationSeat.repository.ReservationSeatRepository;
import back.kalender.domain.booking.seatHold.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    // 예매 세션 생성
    @Transactional
    public CreateReservationResponse createReservation(
            Long scheduleId,
            CreateReservationRequest request,
            Long userId
    ) {
        // TODO:
        // 대기열 토큰 검증
        // Schedule 조회 및 검증
        // Reservation 엔티티 생성 및 저장

        // 더미 응답
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(5);
        long remainingSeconds = 300L;

        return new CreateReservationResponse(
                1L,  // 더미 reservationId
                "HOLD",
                expiresAt,
                remainingSeconds
        );
    }

    // 좌석 홀드
    public HoldSeatsResponse holdSeats(
            Long reservationId,
            HoldSeatsRequest request,
            Long userId
    ) {
        return seatHoldService.holdSeats(reservationId, request, userId);
    }

    // 좌석 홀드 해제
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
        // TODO: Reservation 조회 및 권한 검증
        // TODO: Performance, Schedule, Hall 정보 조회
        // TODO: ReservationSeat 목록 조회
        // TODO: ReservationMapper로 응답 생성

        // 더미 응답
        LocalDateTime now = LocalDateTime.now();
        return new ReservationSummaryResponse(
                reservationId,
                new ReservationSummaryResponse.PerformanceInfo(
                        1L, "BTS 콘서트", "https://poster.jpg", "잠실주경기장"
                ),
                new ReservationSummaryResponse.ScheduleInfo(
                        1L,
                        now.toLocalDate().plusDays(30),
                        now.toLocalTime(),
                        1
                ),
                List.of(
                        new ReservationSummaryResponse.SelectedSeatInfo(
                                101L, 1, "A", 2, 5, "VIP", 150000
                        )
                ),
                150000,
                now.plusMinutes(5),
                280L,
                now.plusDays(30).minusHours(1)
        );
    }

    @Transactional
    public UpdateDeliveryInfoResponse updateDeliveryInfo(
            Long reservationId,
            UpdateDeliveryInfoRequest request,
            Long userId
    ) {

        // TODO: Reservation 조회 및 권한 검증
        // TODO: 배송 정보 유효성 검증
        // TODO: Reservation.updateDeliveryInfo() 호출

        // 더미 응답
        return new UpdateDeliveryInfoResponse(
                reservationId,
                request.deliveryMethod(),
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(3),
                180L
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
