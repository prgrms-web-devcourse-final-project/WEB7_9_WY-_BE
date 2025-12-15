package back.kalender.domain.performance.performanceHall.repository;

import back.kalender.domain.performance.performanceHall.entity.PerformanceHall;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceHallRepository extends JpaRepository<PerformanceHall, Long> {
}
