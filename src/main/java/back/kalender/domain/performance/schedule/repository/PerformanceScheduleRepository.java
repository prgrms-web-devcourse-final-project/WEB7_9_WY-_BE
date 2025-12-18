package back.kalender.domain.performance.schedule.repository;

import back.kalender.domain.performance.performance.entity.Performance;
import back.kalender.domain.performance.schedule.entity.PerformanceSchedule;
import back.kalender.domain.performance.schedule.entity.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PerformanceScheduleRepository extends JpaRepository<PerformanceSchedule, Long> {
    // 특정 공연의 모든 회차 조회
    List<PerformanceSchedule> findAllByPerformanceIdOrderByPerformanceDateAscStartTimeAsc(Long performanceId);

    // 특정 공연의 특정 날짜 회차들만 조회
    List<PerformanceSchedule> findAllByPerformanceIdAndPerformanceDateOrderByStartTimeAsc(
        Long performanceId,
        LocalDate performanceDate
    );

    List<PerformanceSchedule> findByPerformanceId(Long performanceId);

    // 특정 공연의 예매 가능한 날짜 목록 조회 (중복 제거)
    List<LocalDate> findAvailableDatesByPerformanceId(Long performanceId);



        @Query("""
        select ps.id
        from PerformanceSchedule ps
        where ps.status = :status
    """)
        List<Long> findScheduleIdsByStatus(@Param("status") ScheduleStatus status);

}
