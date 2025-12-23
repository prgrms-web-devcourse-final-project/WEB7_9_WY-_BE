package back.kalender.domain.schedule.dto.response;

import back.kalender.domain.schedule.enums.ScheduleCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Optional;

public record ScheduleResponse(
        @Schema(description = "일정 ID", example = "120")
        Long scheduleId,

        @Schema(description = "아티스트 ID", example = "2")
        Long artistId,

        @Schema(description = "아티스트 이름", example = "NewJeans")
        String artistName,

        @Schema(description = "일정 제목", example = "뮤직뱅크 출연")
        String title,

        @Schema(description = "일정 카테고리", example = "BROADCAST")
        ScheduleCategory scheduleCategory,

        @Schema(description = "일정 시작 시간", example = "2025-12-15T17:00:00")
        LocalDateTime scheduleTime,

        @Schema(description = "공연 ID (선택 사항)", example = "null")
        Optional<Long> performanceId,

        @Schema(description = "예매처 또는 관련 링크 (없을 경우 null)", example = "https://ticket.example.com/newjeans")
        Optional<String> link,

        @Schema(description = "일정 장소 (선택 사항)", example = "KBS 신관 공개홀")
        Optional<String> location
) {
        public ScheduleResponse(
                Long scheduleId,
                Long artistId,
                String artistName,
                String title,
                ScheduleCategory scheduleCategory,
                LocalDateTime scheduleTime,
                Long performanceId,
                String link,
                String location
        ) {
                this(
                        scheduleId,
                        artistId,
                        artistName,
                        title,
                        scheduleCategory,
                        scheduleTime,
                        Optional.ofNullable(performanceId),
                        Optional.ofNullable(link),
                        Optional.ofNullable(location)
                );
        }
}