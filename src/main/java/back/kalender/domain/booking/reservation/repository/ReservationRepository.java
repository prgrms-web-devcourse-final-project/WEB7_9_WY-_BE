package back.kalender.domain.booking.reservation.repository;

import back.kalender.domain.booking.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

}
