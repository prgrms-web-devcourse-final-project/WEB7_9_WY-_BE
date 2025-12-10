package back.kalender.domain.performance.repository;

import back.kalender.domain.performance.entity.Performance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerformanceRepository extends JpaRepository<Performance, Long> {

    @Query("SELECT p FROM Performance p " +
            "JOIN FETCH p.performanceHall " +
            "JOIN FETCH p.artist " +
            "WHERE p.id = :performanceId")
    Optional<Performance> findByIdWithDetails(@Param("performanceId") Long performanceId);
}
