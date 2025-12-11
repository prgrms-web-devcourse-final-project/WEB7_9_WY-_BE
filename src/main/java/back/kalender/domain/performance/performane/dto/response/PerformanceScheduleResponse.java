package back.kalender.domain.performance.performane.dto.response;

import back.kalender.domain.performance.schedule.entity.PerformanceSchedule;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "공연 회차 정보")
public record PerformanceScheduleResponse (
        Long scheduleId,
        LocalDate performanceDate,
        LocalTime startTime,
        Integer PerformanceNo,
        String status
){
    public static PerformanceScheduleResponse from(PerformanceSchedule schedule) {
        return new PerformanceScheduleResponse(
                schedule.getId(),
                schedule.getPerformanceDate(),
                schedule.getStartTime(),
                schedule.getPerformanceNo(),
                schedule.getStatus().name()
        );
    }
}
