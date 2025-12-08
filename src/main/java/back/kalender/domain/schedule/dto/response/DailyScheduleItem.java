package back.kalender.domain.schedule.dto.response;

import java.time.LocalDateTime;
import java.util.Optional;

public record DailyScheduleItem(
        Long scheduleId,
        String artistName,
        String title,
        String category,
        LocalDateTime scheduleTime,
        Optional<String> link,
        Optional<Long> performanceId
        //TODO: 카테고리 enum 추가 필요한지?

) {
}
