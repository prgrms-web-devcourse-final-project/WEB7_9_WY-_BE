package back.kalender.domain.performance.repository;

import back.kalender.domain.performance.entity.PerformanceHall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerformanceHallRepository extends JpaRepository<PerformanceHall, Long> {
}
