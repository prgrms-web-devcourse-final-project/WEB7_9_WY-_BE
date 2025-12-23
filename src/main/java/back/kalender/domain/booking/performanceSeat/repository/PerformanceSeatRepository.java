package back.kalender.domain.booking.performanceSeat.repository;

import back.kalender.domain.booking.performanceSeat.dto.PerformanceSeatResponse;
import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PerformanceSeatRepository extends JpaRepository<PerformanceSeat, Long> {

    @Query("""
select new back.kalender.domain.booking.performanceSeat.dto.PerformanceSeatResponse(
    p.id,
    p.floor,
    p.block,
    p.rowNumber,
    p.seatNumber,
    p.x,
    p.y,
    p.priceGradeId
)
from PerformanceSeat p
where p.scheduleId = :scheduleId
""")
    List<PerformanceSeatResponse> findSeatResponses(
            @Param("scheduleId") Long scheduleId
    );


    List<PerformanceSeat> findByScheduleIdAndBlock(Long scheduleId, String block);

    Optional<PerformanceSeat> findByIdAndScheduleId(Long id, Long scheduleId);

    boolean existsByScheduleId(Long id);
}
