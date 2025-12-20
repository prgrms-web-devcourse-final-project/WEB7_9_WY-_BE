package back.kalender.domain.schedule.mapper;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.schedule.dto.response.EventResponse;
import back.kalender.domain.schedule.dto.response.ScheduleResponse;
import back.kalender.domain.schedule.dto.response.UpcomingEventResponse;
import back.kalender.domain.schedule.entity.Schedule;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ScheduleResponseMapper {

    private ScheduleResponseMapper() {}

    public static UpcomingEventResponse toUpcomingEventResponse(
            Schedule schedule,
            Artist artist,
            LocalDate today
    ) {
        long daysUntilEvent = ChronoUnit.DAYS.between(today, schedule.getScheduleTime().toLocalDate());

        return new UpcomingEventResponse(
                schedule.getId(),
                artist.getName(),
                schedule.getTitle(),
                schedule.getScheduleCategory(),
                schedule.getScheduleTime(),
                schedule.getPerformanceId(),
                schedule.getLink(),
                daysUntilEvent,
                schedule.getLocation()
        );
    }

    public static EventResponse toEventResponse(Schedule schedule, Artist artist) {
        String formattedTitle = "[" + artist.getName() + "] " + schedule.getTitle();

        return new EventResponse(
                schedule.getId(),
                formattedTitle
        );
    }
    public static ScheduleResponse toScheduleResponse(Schedule schedule, Artist artist) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getArtistId(),
                artist.getName(),
                schedule.getTitle(),
                schedule.getScheduleCategory(),
                schedule.getScheduleTime(),
                schedule.getPerformanceId(),
                schedule.getLink(),
                schedule.getLocation()
        );
    }
}