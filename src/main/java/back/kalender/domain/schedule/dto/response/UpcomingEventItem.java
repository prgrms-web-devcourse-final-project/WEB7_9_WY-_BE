package back.kalender.domain.schedule.dto.response;

import back.kalender.domain.schedule.entity.ScheduleCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Optional;

public record UpcomingEventItem(
        @Schema(description = "일정 ID", example = "205")
        Long scheduleId,

        @Schema(description = "아티스트 이름", example = "aespa")
        String artistName,

        @Schema(description = "일정 제목", example = "aespa 미니 4집 팬사인회")
        String title,

        @Schema(description = "일정 카테고리", example = "FAN_SIGN")
        ScheduleCategory scheduleCategory,

        @Schema(description = "일정 시작 시간", example = "2025-12-20T14:00:00")
        LocalDateTime scheduleTime,

        @Schema(description = "공연 ID (선택 사항)", example = "null")
        Optional<Long> performanceId,

        @Schema(description = "신청/예매 링크 (선택 사항)", example = "https://fansign.example.com/aespa")
        Optional<String> link,

        @Schema(description = "일정까지 남은 일수 (D-Day)", example = "5")
        Long daysUntilEvent,

        @Schema(description = "일정 장소 (선택 사항)", example = "코엑스 라이브플라자")
        Optional<String> location
) {
        public UpcomingEventItem(
                Long scheduleId,
                String artistName,
                String title,
                ScheduleCategory scheduleCategory,
                LocalDateTime scheduleTime,
                Long performanceId,
                String link,
                Long daysUntilEvent,
                String location
        ) {
                this(
                        scheduleId,
                        artistName,
                        title,
                        scheduleCategory,
                        scheduleTime,
                        Optional.ofNullable(performanceId),
                        Optional.ofNullable(link),
                        daysUntilEvent,
                        Optional.ofNullable(location)
                );
        }
}