package back.kalender.domain.party.dto.query;

import back.kalender.domain.schedule.enums.ScheduleCategory;

import java.time.LocalDateTime;

public record NotificationTarget(
        Long userId,
        Long partyId,
        String scheduleTitle,
        ScheduleCategory category,
        LocalDateTime scheduleTime
) {
}
