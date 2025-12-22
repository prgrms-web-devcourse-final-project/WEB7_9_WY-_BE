package back.kalender.domain.booking.seatHold.exception;

import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import lombok.Getter;

/**
 * 좌석 선점 불가 예외
 *
 * <발생 상황>
 * 다른 사용자가 HOLD 중 (TTL 아직 만료X)
 * 이미 SOLD 상태
 */

@Getter
public class SeatNotAvailableException extends RuntimeException {

    private final Long seatId;
    private final SeatStatus currentStatus;
    private final String reason;

    public SeatNotAvailableException(Long seatId, SeatStatus currentStatus, String reason) {
        super(String.format("좌석 선점 불가 - seatId: %d, status: %s, reason: %s",
                seatId, currentStatus, reason));
        this.seatId = seatId;
        this.currentStatus = currentStatus;
        this.reason = reason;
    }
}
