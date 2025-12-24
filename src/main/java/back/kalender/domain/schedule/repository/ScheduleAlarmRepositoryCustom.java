package back.kalender.domain.schedule.repository;

import back.kalender.domain.party.dto.query.NotificationTarget;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleAlarmRepositoryCustom {
    List<NotificationTarget> findScheduleNotificationTargets(LocalDateTime start, LocalDateTime end);
}
