package back.kalender.domain.schedule.dto.response;

import back.kalender.domain.schedule.entity.ScheduleCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public record MonthlyScheduleItem(
        @Schema(description = "일정 ID", example = "101")
        Long scheduleId,

        @Schema(description = "아티스트 ID", example = "2")
        Long artistId,

        @Schema(description = "아티스트 이름", example = "BTS")
        String artistName,

        @Schema(description = "일정 제목", example = "BTS 월드 투어 서울 공연")
        String title,

        @Schema(description = "일정 카테고리 (CONCERT, BROADCAST, FAN_MEETING 등)", example = "CONCERT")
        ScheduleCategory scheduleCategory,

        @Schema(description = "일정 시작 시간", example = "2025-11-02T19:00:00")
        LocalDateTime scheduleTime,

        @Schema(description = "공연 ID (공연 일정인 경우 포함, 없을 경우 null)", example = "501")
        Optional<Long> performanceId,

        @Schema(description = "일정 장소 (선택 사항)", example = "고척 스카이돔")
        Optional<String> location
) {
        public MonthlyScheduleItem(
                Long scheduleId,
                Long artistId,
                String artistName,
                String title,
                ScheduleCategory scheduleCategory,
                LocalDateTime scheduleTime,
                Long performanceId,
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
                        Optional.ofNullable(location)
                );
        }
}