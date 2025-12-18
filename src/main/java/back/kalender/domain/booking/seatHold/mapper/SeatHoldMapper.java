package back.kalender.domain.booking.seatHold.mapper;

import back.kalender.domain.booking.seatHold.entity.SeatHoldLog;

import java.time.LocalDateTime;

public class SeatHoldMapper {
    public static SeatHoldLog toHoldLog(
            Long performanceSeatId,
            Long userId,
            LocalDateTime holdStartedAt,
            LocalDateTime holdExpiredAt
    ) {
        return SeatHoldLog.builder()
                .performanceSeatId(performanceSeatId)
                .userId(userId)
                .holdStartedAt(holdStartedAt)
                .holdExpiredAt(holdExpiredAt)
                .build();
    }

    public static SeatHoldLog toReleaseLog(
            Long performanceSeatId,
            Long userId
    ) {
        LocalDateTime now = LocalDateTime.now();
        SeatHoldLog log = SeatHoldLog.builder()
                .performanceSeatId(performanceSeatId)
                .userId(userId)
                .holdStartedAt(now)
                .holdExpiredAt(now)
                .build();

        log.markReleased();
        return log;
    }
}
