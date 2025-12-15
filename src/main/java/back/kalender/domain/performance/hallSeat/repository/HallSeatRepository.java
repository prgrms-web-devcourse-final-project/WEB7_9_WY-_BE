package back.kalender.domain.performance.hallSeat.repository;

import back.kalender.domain.performance.hallSeat.entity.HallSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HallSeatRepository extends JpaRepository<HallSeat, Long> {

    List<HallSeat> findByPerformanceHall_Id(Long hallId);

}
