package back.kalender.domain.schedule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record DailySchedulesResponse(
        @Schema(description = "해당 날짜의 상세 일정 목록")
        List<DailyScheduleItem> dailySchedules
) {
}
