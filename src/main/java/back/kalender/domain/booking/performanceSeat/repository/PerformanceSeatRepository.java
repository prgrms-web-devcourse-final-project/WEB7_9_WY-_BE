package back.kalender.domain.booking.performanceSeat.repository;

import back.kalender.domain.booking.performanceSeat.dto.PerformanceSeatResponse;
import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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


    Optional<PerformanceSeat> findByIdAndScheduleId(Long id, Long scheduleId);

    @Query("""
        select p
        from PerformanceSeat p
        where p.status = :status
          and p.holdExpiredAt is not null
          and p.holdExpiredAt < :now
        order by p.holdExpiredAt asc
    """)
    List<PerformanceSeat> findExpiredHoldSeats(
            @Param("status") SeatStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
