package back.kalender.domain.schedule.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record EventResponse(
        @Schema(description = "일정 ID", example = "120")
        Long scheduleId,

        @Schema(description = "일정 제목", example = "뮤직뱅크 출연")
        String title,

        @Schema(description = "아티스트 명", example = "NewJeans")
        String artistName
) {
}
