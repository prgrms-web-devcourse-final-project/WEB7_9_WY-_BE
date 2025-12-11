package back.kalender.domain.booking.performanceSeat.repository;

import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceSeatRepository extends JpaRepository<PerformanceSeat, Long> {

    List<PerformanceSeat> findByScheduleId(Long scheduleId);

    List<PerformanceSeat> findByScheduleIdAndBlock(Long scheduleId, String block);
}
