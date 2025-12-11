package back.kalender.domain.schedule.dto.response;

import java.util.List;

public record EventsListResponse(
        List<EventResponse> events
) {
}
