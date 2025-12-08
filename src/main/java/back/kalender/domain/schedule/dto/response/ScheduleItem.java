package back.kalender.domain.schedule.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public record ScheduleItem(
        Long scheduleId,
        Long artistId,
        String artistName,
        String title,
        String category,
        Optional<Long> performanceId,
        LocalDateTime scheduleTime,
        LocalDate date

) {
}