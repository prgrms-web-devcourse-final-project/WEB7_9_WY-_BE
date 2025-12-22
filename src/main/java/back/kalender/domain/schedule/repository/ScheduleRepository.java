package back.kalender.domain.schedule.repository;

import back.kalender.domain.schedule.entity.Schedule;
import back.kalender.domain.schedule.enums.ScheduleCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query("""
        SELECT s
        FROM Schedule s
        WHERE s.artistId IN :artistIds
        AND s.scheduleTime BETWEEN :startDateTime AND :endDateTime
        ORDER BY s.scheduleTime ASC
    """)
    List<Schedule> findMonthlySchedules(
            @Param("artistIds") List<Long> artistIds,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query("""
        SELECT s
        FROM Schedule s
        WHERE s.artistId IN :artistIds
        AND s.scheduleTime >= :now
        ORDER BY s.scheduleTime ASC
    """)
    List<Schedule> findUpcomingEvents(
            @Param("artistIds") List<Long> artistIds,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("""
        SELECT s
        FROM Schedule s
        WHERE s.artistId IN :artistIds
        AND s.scheduleCategory IN :categories
        AND s.scheduleTime > :now
        AND s.scheduleTime <= :endDate
        ORDER BY s.scheduleTime ASC
    """)
    List<Schedule> findPartyAvailableEvents(
            @Param("artistIds") List<Long> artistIds,
            @Param("categories") List<ScheduleCategory> categories,
            @Param("now") LocalDateTime now,
            @Param("endDate") LocalDateTime endDate
    );

    List<Schedule> findAllByScheduleTimeBetween(LocalDateTime start, LocalDateTime end);

    boolean existsByArtistId(Long id);

    List<Schedule> findByScheduleCategoryAndPerformanceIdIsNotNull(ScheduleCategory scheduleCategory);
}
