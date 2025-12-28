package back.kalender.domain.booking.seatHold.event;

import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 좌석 홀드 db작업 완료 이벤트
@Getter
@RequiredArgsConstructor
public class SeatHoldCompletedEvent {
    private final Long scheduleId;
    private final Long seatId;
    private final Long userId;
    private final SeatStatus status;
    private final Long holdTtlSeconds;

    @Override
    public String toString() {
        return String.format("SeatHoldCompletedEvent[scheduleId=%d, seatId=%d, userId=%d, status=%s]",
                scheduleId, seatId, userId, status);
    }
}
