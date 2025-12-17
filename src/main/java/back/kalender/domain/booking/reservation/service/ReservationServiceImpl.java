package back.kalender.domain.booking.reservation.service;

import back.kalender.domain.booking.reservation.dto.request.CreateReservationRequest;
import back.kalender.domain.booking.reservation.dto.request.HoldSeatsRequest;
import back.kalender.domain.booking.reservation.dto.request.ReleaseSeatsRequest;
import back.kalender.domain.booking.reservation.dto.request.UpdateDeliveryInfoRequest;
import back.kalender.domain.booking.reservation.dto.response.*;
import back.kalender.domain.booking.reservation.repository.ReservationRepository;
import back.kalender.domain.booking.reservationSeat.repository.ReservationSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;

    @Override
    @Transactional
    public CreateReservationResponse createReservation(
            Long scheduleId,
            CreateReservationRequest request,
            Long userId
    ) {
        // TODO: 대기열 토큰 검증 (A파트 서비스 호출)
        // TODO: Reservation 엔티티 생성 및 저장

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

    @Override
    @Transactional
    public HoldSeatsResponse holdSeats(
            Long reservationId,
            HoldSeatsRequest request,
            Long userId
    ) {

        // TODO: Reservation 조회 및 권한 검증
        // TODO: 만료 시간 검증
        // TODO: Redis 분산 락으로 좌석 선점
        // TODO: 원자적 처리 (하나라도 실패 시 전체 롤백)
        // TODO: ReservationSeat 생성
        // TODO: 총액 계산 및 업데이트

        // 더미 응답 (성공 케이스)
        LocalDateTime now = LocalDateTime.now();
        return new HoldSeatsResponse(
                reservationId,
                "HOLD",
                List.of(
                        new HoldSeatsResponse.HeldSeatInfo(
                                101L, 1, "A", 2, 5, "VIP", 150000
                        ),
                        new HoldSeatsResponse.HeldSeatInfo(
                                102L, 1, "A", 2, 6, "VIP", 150000
                        )
                ),
                300000,
                now.plusMinutes(5),
                280L
        );
    }

    @Override
    @Transactional
    public ReleaseSeatsResponse releaseSeats(
            Long reservationId,
            ReleaseSeatsRequest request,
            Long userId
    ) {

        // TODO: Reservation 조회 및 권한 검증
        // TODO: ReservationSeat 삭제
        // TODO: PerformanceSeat 상태 AVAILABLE로 복원
        // TODO: 총액 재계산

        // 더미 응답
        LocalDateTime now = LocalDateTime.now();
        return new ReleaseSeatsResponse(
                reservationId,
                "HOLD",
                request.performanceSeatIds(),
                1,  // 남은 좌석 수
                150000,  // 재계산된 총액
                now.plusMinutes(5),
                260L
        );
    }

    @Override
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

    @Override
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

    @Override
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
}
