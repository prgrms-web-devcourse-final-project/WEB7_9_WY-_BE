package back.kalender.domain.booking.reservationSeat.repository;

import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, Long> {
}
