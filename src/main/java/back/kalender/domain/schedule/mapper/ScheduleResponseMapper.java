package back.kalender.domain.schedule.mapper;

import back.kalender.domain.schedule.dto.response.UpcomingEventResponse;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ScheduleResponseMapper {

    private ScheduleResponseMapper() {}

    public static UpcomingEventResponse toUpcomingEventResponse(UpcomingEventResponse rawItem, LocalDate today) {
        long daysUntilEvent = ChronoUnit.DAYS.between(today, rawItem.scheduleTime().toLocalDate());

        return new UpcomingEventResponse(
                rawItem.scheduleId(),
                rawItem.artistName(),
                rawItem.title(),
                rawItem.scheduleCategory(),
                rawItem.scheduleTime(),
                rawItem.performanceId(),
                rawItem.link(),
                daysUntilEvent,
                rawItem.location()
        );
    }
}