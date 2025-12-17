package back.kalender.domain.booking.reservation.service;

import back.kalender.domain.booking.reservation.dto.request.CreateReservationRequest;
import back.kalender.domain.booking.reservation.dto.request.HoldSeatsRequest;
import back.kalender.domain.booking.reservation.dto.request.ReleaseSeatsRequest;
import back.kalender.domain.booking.reservation.dto.request.UpdateDeliveryInfoRequest;
import back.kalender.domain.booking.reservation.dto.response.*;

public interface ReservationService {

    // 예매 세션 생성
    CreateReservationResponse createReservation(
            Long scheduleId,
            CreateReservationRequest request,
            Long userId
    );

    /**
     * 좌석 선점 (Hold)
     * - 전체 성공 or 전체 실패
     * - 충돌 시 HoldSeatsConflictException 발생
     */
    HoldSeatsResponse holdSeats(
            Long reservationId,
            HoldSeatsRequest request,
            Long userId
    );

    // 좌석 선점 해제 (Release)
    ReleaseSeatsResponse releaseSeats(
            Long reservationId,
            ReleaseSeatsRequest request,
            Long userId
    );

    // 예매 요약 조회
    ReservationSummaryResponse getReservationSummary(
            Long reservationId,
            Long userId
    );

    // 배송/수령 정보 저장
    UpdateDeliveryInfoResponse updateDeliveryInfo(
            Long reservationId,
            UpdateDeliveryInfoRequest request,
            Long userId
    );

    // 예매 취소
    void cancelReservation(
            Long reservationId,
            Long userId
    );
}
