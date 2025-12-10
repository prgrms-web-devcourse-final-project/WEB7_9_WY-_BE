package back.kalender.domain.schedule.repository;

import back.kalender.domain.schedule.dto.response.DailyScheduleItem;
import back.kalender.domain.schedule.dto.response.MonthlyScheduleItem;
import back.kalender.domain.schedule.dto.response.UpcomingEventItem;
import back.kalender.domain.schedule.entity.Schedule;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query("""
        SELECT new back.kalender.domain.schedule.dto.response.MonthlyScheduleItem(
            s.id,
            s.artistId,
            a.name,
            s.title,
            s.scheduleCategory,
            s.scheduleTime,
            s.performanceId,
            CAST(s.scheduleTime AS LocalDate),
            s.location
        )
        FROM Schedule s
        JOIN ArtistTmp a ON s.artistId = a.id
        WHERE s.artistId IN :artistIds
        AND s.scheduleTime BETWEEN :startDateTime AND :endDateTime
        ORDER BY s.scheduleTime ASC
    """)
    List<MonthlyScheduleItem> findMonthlySchedules(
            @Param("artistIds") List<Long> artistIds,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query("""
        SELECT new back.kalender.domain.schedule.dto.response.DailyScheduleItem(
            s.id,
            a.name,
            s.title,
            s.scheduleCategory,
            s.scheduleTime,
            s.performanceId,
            s.link,
            s.location
        )
        FROM Schedule s
        JOIN ArtistTmp a ON s.artistId = a.id
        WHERE s.artistId IN :artistIds
        AND s.scheduleTime BETWEEN :startOfDay AND :endOfDay
        ORDER BY s.scheduleTime ASC
    """)
    List<DailyScheduleItem> findDailySchedules(
            @Param("artistIds") List<Long> artistIds,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    @Query("""
        SELECT new back.kalender.domain.schedule.dto.response.UpcomingEventItem(
            s.id,
            a.name,
            s.title,
            s.scheduleCategory,
            s.scheduleTime,
            s.performanceId,
            s.link,
            CAST(NULL AS Long),
            s.location
        )
        FROM Schedule s
        JOIN ArtistTmp a ON s.artistId = a.id
        WHERE s.artistId IN :artistIds
        AND s.scheduleTime >= :now
        ORDER BY s.scheduleTime ASC
    """)
    List<UpcomingEventItem> findUpcomingEvents(
            @Param("artistIds") List<Long> artistIds,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
