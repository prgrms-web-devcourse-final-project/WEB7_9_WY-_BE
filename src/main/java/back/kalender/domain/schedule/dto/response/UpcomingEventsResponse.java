package back.kalender.domain.schedule.dto.response;

import java.util.List;

public record UpcomingEventsResponse(
        List<UpcomingEventItem> upcomingEvents
) {
}
