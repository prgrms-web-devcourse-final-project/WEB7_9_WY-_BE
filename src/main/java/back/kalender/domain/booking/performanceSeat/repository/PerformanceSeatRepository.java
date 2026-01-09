package back.kalender.domain.booking.performanceSeat.repository;

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

    // ---- 기존 ----
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

    // =========================
    // 추가: 블록/서브블록 집계 + 상세 조회 (성능용)
    // =========================

    // 1) 블록별 총 좌석 수
    @Query("""
        select p.floor as floor, p.block as block, count(p) as cnt
        from PerformanceSeat p
        where p.scheduleId = :scheduleId
        group by p.floor, p.block
    """)
    List<BlockCountView> countTotalByBlock(@Param("scheduleId") Long scheduleId);

    // 1-2) 블록별 DB 기준 "AVAILABLE" 좌석 수 (Redis 반영 전)
    @Query("""
        select p.floor as floor, p.block as block, count(p) as cnt
        from PerformanceSeat p
        where p.scheduleId = :scheduleId
          and p.status = :availableStatus
        group by p.floor, p.block
    """)
    List<BlockCountView> countDbAvailableByBlock(
            @Param("scheduleId") Long scheduleId,
            @Param("availableStatus") SeatStatus availableStatus
    );

    // 1-3) 블록별 "override 차감"용: (Redis SOLD/HOLD에 걸린 좌석 중) DB에서 AVAILABLE로 잡힌 개수
    @Query("""
        select p.floor as floor, p.block as block, count(p) as cnt
        from PerformanceSeat p
        where p.scheduleId = :scheduleId
          and p.status = :availableStatus
          and p.id in :seatIds
        group by p.floor, p.block
    """)
    List<BlockCountView> countDbAvailableOverridesByBlock(
            @Param("scheduleId") Long scheduleId,
            @Param("availableStatus") SeatStatus availableStatus,
            @Param("seatIds") List<Long> seatIds
    );

    // 2) 서브블록별 총 좌석 수
    @Query("""
        select p.subBlock as subBlock, count(p) as cnt
        from PerformanceSeat p
        where p.scheduleId = :scheduleId
          and p.block = :block
        group by p.subBlock
    """)
    List<SubBlockCountView> countTotalBySubBlock(
            @Param("scheduleId") Long scheduleId,
            @Param("block") String block
    );

    // 2-2) 서브블록별 DB 기준 AVAILABLE 수
    @Query("""
        select p.subBlock as subBlock, count(p) as cnt
        from PerformanceSeat p
        where p.scheduleId = :scheduleId
          and p.block = :block
          and p.status = :availableStatus
        group by p.subBlock
    """)
    List<SubBlockCountView> countDbAvailableBySubBlock(
            @Param("scheduleId") Long scheduleId,
            @Param("block") String block,
            @Param("availableStatus") SeatStatus availableStatus
    );

    // 2-3) 서브블록별 override 차감용
    @Query("""
        select p.subBlock as subBlock, count(p) as cnt
        from PerformanceSeat p
        where p.scheduleId = :scheduleId
          and p.block = :block
          and p.status = :availableStatus
          and p.id in :seatIds
        group by p.subBlock
    """)
    List<SubBlockCountView> countDbAvailableOverridesBySubBlock(
            @Param("scheduleId") Long scheduleId,
            @Param("block") String block,
            @Param("availableStatus") SeatStatus availableStatus,
            @Param("seatIds") List<Long> seatIds
    );

    // 3) 상세 조회 (해당 subBlock 좌석만) - 엔티티 전체 로딩 대신 Projection
    @Query("""
        select p.id as seatId,
               p.rowNumber as rowNumber,
               p.seatNumber as seatNumber,
               p.priceGradeId as priceGradeId
        from PerformanceSeat p
        where p.scheduleId = :scheduleId
          and p.block = :block
          and p.subBlock = :subBlock
        order by p.rowNumber asc, p.seatNumber asc
    """)
    List<SeatDetailView> findSeatDetails(
            @Param("scheduleId") Long scheduleId,
            @Param("block") String block,
            @Param("subBlock") String subBlock
    );

    // ----- Projection interfaces -----
    interface BlockCountView {
        int getFloor();
        String getBlock();
        long getCnt();
    }

    interface SubBlockCountView {
        String getSubBlock();
        long getCnt();
    }

    interface SeatDetailView {
        Long getSeatId();
        int getRowNumber();
        int getSeatNumber();
        Long getPriceGradeId();
    }
}
