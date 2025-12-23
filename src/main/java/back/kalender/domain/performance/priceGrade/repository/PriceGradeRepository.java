package back.kalender.domain.performance.priceGrade.repository;

import back.kalender.domain.performance.performance.entity.Performance;
import back.kalender.domain.performance.priceGrade.entity.PriceGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceGradeRepository extends JpaRepository<PriceGrade, Long> {
    List<PriceGrade> findAllByPerformanceId(Long performanceId);

    List<PriceGrade> findByPerformanceId(Long performanceId);
}
