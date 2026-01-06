package back.kalender.domain.booking.performanceSeat.repository;

import back.kalender.domain.booking.performanceSeat.dto.BlockSummaryResponse;
import back.kalender.domain.booking.performanceSeat.dto.SeatDetailResponse;
import back.kalender.domain.booking.performanceSeat.dto.SubBlockSummaryResponse;
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


    List<PerformanceSeat> findAllByScheduleId(Long scheduleId);

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

    @Query("""
select new back.kalender.domain.booking.performanceSeat.dto.BlockSummaryResponse(
    p.floor,
    p.block,
    count(p),
    sum(case when p.status = 'AVAILABLE' then 1 else 0 end)
)
from PerformanceSeat p
where p.scheduleId = :scheduleId
group by p.floor, p.block
""")
    List<BlockSummaryResponse> findBlockSummaries(
            @Param("scheduleId") Long scheduleId
    );

    @Query("""
select new back.kalender.domain.booking.performanceSeat.dto.SubBlockSummaryResponse(
    p.subBlock,
    count(p),
    sum(case when p.status = 'AVAILABLE' then 1 else 0 end)
)
from PerformanceSeat p
where p.scheduleId = :scheduleId
  and p.block = :block
group by p.subBlock
""")
    List<SubBlockSummaryResponse> findSubBlockSummaries(
            @Param("scheduleId") Long scheduleId,
            @Param("block") String block
    );

    @Query("""
select new back.kalender.domain.booking.performanceSeat.dto.SeatDetailResponse(
    p.id,
    p.rowNumber,
    p.seatNumber,
    p.priceGradeId
)
from PerformanceSeat p
where p.scheduleId = :scheduleId
  and p.block = :block
  and p.subBlock = :subBlock
""")
    List<SeatDetailResponse> findSeatDetails(
            @Param("scheduleId") Long scheduleId,
            @Param("block") String block,
            @Param("subBlock") String subBlock
    );

}
