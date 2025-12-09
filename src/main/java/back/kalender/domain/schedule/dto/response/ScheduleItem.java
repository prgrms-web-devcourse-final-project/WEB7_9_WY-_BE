package back.kalender.domain.schedule.dto.response;

import back.kalender.domain.schedule.entity.ScheduleCategory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public record ScheduleItem(
        Long scheduleId,
        Long artistId,
        String artistName,
        String title,
        ScheduleCategory scheduleCategory,
        Optional<Long> performanceId,
        LocalDateTime scheduleTime,
        LocalDate date

) {
}