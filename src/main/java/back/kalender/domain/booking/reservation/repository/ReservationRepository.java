package back.kalender.domain.booking.reservation.repository;

import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservation.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    // 사용자별 완료된 예매 내역 조회 (PAID + CANCELLED만)
    Page<Reservation> findByUserIdAndStatusInOrderByCreatedAtDesc(
            Long userId,
            List<ReservationStatus> statuses,
            Pageable pageable
    );
}
