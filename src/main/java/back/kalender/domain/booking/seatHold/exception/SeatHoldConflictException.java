package back.kalender.domain.booking.seatHold.exception;

import back.kalender.domain.booking.reservation.dto.response.HoldSeatsFailResponse;
import lombok.Getter;

import java.util.List;

/**
 * 좌석 충돌 예외 (409 Conflict)
 *
 * <발생 상황>
 * 락 획득 실패
 * 좌석 선점 불가 (HOLD/SOLD 충돌)
 *
 * <응답>
 * HoldSeatsFailResponse로 변환되어 프론트에 전달
 */

@Getter
public class SeatHoldConflictException extends RuntimeException {
    private final Long reservationId;
    private final List<HoldSeatsFailResponse.ConflictSeat> conflicts;

    public SeatHoldConflictException(Long reservationId, List<HoldSeatsFailResponse.ConflictSeat> conflicts) {
        super("좌석 충돌 발생 - 총 " + conflicts.size() + "개 좌석");
        this.reservationId = reservationId;
        this.conflicts = conflicts;
    }
}
