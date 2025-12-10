package back.kalender.domain.schedule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record UpcomingEventsResponse(
        @Schema(description = "다가오는 이벤트 목록 (시간순 정렬)")
        List<UpcomingEventItem> upcomingEvents
) {
}
