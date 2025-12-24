package back.kalender.domain.schedule.repository;

import back.kalender.domain.schedule.entity.ScheduleAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScheduleAlarmRepository extends JpaRepository<ScheduleAlarm, Long>, ScheduleAlarmRepositoryCustom {
    Optional<ScheduleAlarm> findByScheduleIdAndUserId(Long scheduleId, Long userId);
}