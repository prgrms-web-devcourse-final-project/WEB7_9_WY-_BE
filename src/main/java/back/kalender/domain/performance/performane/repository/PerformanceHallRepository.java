package back.kalender.domain.performance.performane.repository;

import back.kalender.domain.performance.performanceHall.entity.PerformanceHall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerformanceHallRepository extends JpaRepository<PerformanceHall, Long> {
}
