package back.kalender.domain.schedule.dto.response;

import back.kalender.domain.schedule.entity.ScheduleCategory;

import java.time.LocalDateTime;
import java.util.Optional;

public record DailyScheduleItem(
        Long scheduleId,
        String artistName,
        String title,
        ScheduleCategory scheduleCategory,
        LocalDateTime scheduleTime,
        Optional<String> link,
        Optional<Long> performanceId
        //TODO: 예외 코드 작성
        // 아티스트 레포 받아오기 / 아티스트 엔티티 받아오기 (굳이? 아직 뼈대 작업인데 아티스트 받아올 필요 있나?)

) {
}
