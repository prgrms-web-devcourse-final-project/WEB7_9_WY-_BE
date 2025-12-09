package back.kalender.domain.schedule.dto.response;

import back.kalender.domain.schedule.entity.ScheduleCategory;

import java.time.LocalDateTime;
import java.util.Optional;

public record UpcomingEventItem(
        Long scheduleId,
        String artistName,
        String title,
        ScheduleCategory scheduleCategory,
        LocalDateTime scheduleTime,
        Optional<Long> performanceId,
        Optional<String> link,
        Long daysUntilEvent,
        Optional<String> location
) {
}
