package back.kalender.domain.booking.reservationSeat.repository;

import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, Long> {

    // 예매 id로 좌석 목록 조회
    List<ReservationSeat> findByReservationId(Long reservationId);

    // 예매 id로 전제 삭제
    void deleteByReservationId(Long reservationId);

    // 특정 좌석들 삭제
    void deleteByReservationIdAndPerformanceSeatIdIn(
            Long reservationId,
            List<Long> performanceSeatIds
    );

    // 예매별 좌석 수 조회
    @Query("""
        SELECT rs.reservationId, COUNT(rs)
        FROM ReservationSeat rs
        WHERE rs.reservationId IN :reservationIds
        GROUP BY rs.reservationId
        """)
    List<Object[]> countByReservationIds(@Param("reservationIds") List<Long> reservationIds);

    // 만료된 performanceSeatIds로 reservationSeat 조회
    List<ReservationSeat> findByPerformanceSeatIdIn(Collection<Long> performanceSeatIds);
}
