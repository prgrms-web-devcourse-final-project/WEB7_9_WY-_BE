package back.kalender.domain.schedule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record IntegratedSchedulesListResponse(
        @Schema(description = "캘린더에 표시할 월별 일정 목록 (상세 정보 포함)")
        List<ScheduleResponse> monthlySchedules,

        @Schema(description = "사이드바 등에 표시할 다가오는 일정 목록")
        List<UpcomingEventResponse> upcomingEvents
) {
}
