package back.kalender.domain.schedule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MonthlySchedulesResponse(
        @Schema(description = "월별 일정 목록")
        List<MonthlyScheduleItem> schedules
) {
}
