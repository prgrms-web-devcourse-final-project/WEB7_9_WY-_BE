package back.kalender.domain.schedule.repository;

import back.kalender.domain.schedule.entity.ScheduleAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface ScheduleAlarmRepository extends JpaRepository<ScheduleAlarm, Long>, ScheduleAlarmRepositoryCustom {
    Optional<ScheduleAlarm> findByScheduleIdAndUserId(Long scheduleId, Long userId);

    @Query("SELECT sa.scheduleId FROM ScheduleAlarm sa WHERE sa.userId = :userId")
    Set<Long> findScheduleIdsByUserId(@Param("userId") Long userId);
}