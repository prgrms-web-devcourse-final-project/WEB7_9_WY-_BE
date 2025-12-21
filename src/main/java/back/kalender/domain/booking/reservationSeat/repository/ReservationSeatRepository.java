package back.kalender.domain.booking.reservationSeat.repository;

import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;

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

    List<Object[]> countByReservationIds(List<Long> reservationIds);
}
