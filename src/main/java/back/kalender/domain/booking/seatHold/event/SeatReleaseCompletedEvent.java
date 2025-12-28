package back.kalender.domain.booking.seatHold.event;

import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 좌석 홀드 해제 db 작업 완료 이벤트
@Getter
@RequiredArgsConstructor
public class SeatReleaseCompletedEvent {
    private final Long scheduleId;
    private final Long seatId;
    private final Long userId;
    private final SeatStatus status;

    @Override
    public String toString() {
        return String.format("SeatReleaseCompletedEvent[scheduleId=%d, seatId=%d, userId=%d, status=%s]",
                scheduleId, seatId, userId, status);
    }
}
