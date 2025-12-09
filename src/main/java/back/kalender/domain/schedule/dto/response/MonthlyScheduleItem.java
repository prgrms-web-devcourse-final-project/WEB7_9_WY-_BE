package back.kalender.domain.schedule.dto.response;

import back.kalender.domain.schedule.entity.ScheduleCategory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public record MonthlyScheduleItem(
        Long scheduleId,
        Long artistId,
        String artistName,
        String title,
        ScheduleCategory scheduleCategory,
        LocalDateTime scheduleTime,
        Optional<Long> performanceId,
        LocalDate date,

        Optional<String> location
) {
}