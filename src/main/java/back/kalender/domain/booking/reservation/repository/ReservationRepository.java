package back.kalender.domain.booking.reservation.repository;

import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservation.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    // 사용자별 완료된 예매 내역 조회 (PAID + CANCELLED만)
    Page<Reservation> findByUserIdAndStatusInOrderByCreatedAtDesc(
            Long userId,
            List<ReservationStatus> statuses,
            Pageable pageable
    );

    boolean existsByUserIdAndPerformanceScheduleIdAndStatusIn(
            Long userId,
            Long performanceScheduleId,
            Collection<ReservationStatus> statuses
    );

    // 만료된 HOLD 예매 조회
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.status = 'HOLD'
        AND r.expiresAt < :now
        ORDER BY r.expiresAt ASC
        """)
    List<Reservation> findExpiredHoldReservations(
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
