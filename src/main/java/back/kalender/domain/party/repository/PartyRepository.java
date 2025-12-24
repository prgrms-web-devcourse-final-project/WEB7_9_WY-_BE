package back.kalender.domain.party.repository;

import back.kalender.domain.party.entity.Party;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Long>, PartyRepositoryCustom {

    @Query("SELECT p FROM Party p WHERE p.leaderId = :leaderId " +
            "AND p.status NOT IN ('COMPLETED', 'CANCELLED') " +
            "ORDER BY p.createdAt DESC")
    Page<Party> findActivePartiesByLeaderId(@Param("leaderId") Long leaderId, Pageable pageable);

    List<Party> findAllByScheduleId(Long scheduleId);

    List<Party> findAllByScheduleIdIn(List<Long> scheduleIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Party p WHERE p.id = :id")
    Optional<Party> findByIdWithLock(@Param("id") Long id);
}