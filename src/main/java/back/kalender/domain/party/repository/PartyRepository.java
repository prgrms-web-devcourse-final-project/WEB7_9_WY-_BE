package back.kalender.domain.party.repository;

import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.entity.PartyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartyRepository extends JpaRepository<Party, Long>, PartyRepositoryCustom {

    Page<Party> findByLeaderIdAndStatus(Long leaderId, PartyStatus status, Pageable pageable);

    @Query("SELECT p FROM Party p WHERE p.leaderId = :leaderId " +
            "AND p.status NOT IN ('COMPLETED', 'CANCELLED') " +
            "ORDER BY p.createdAt DESC")
    Page<Party> findActivePartiesByLeaderId(@Param("leaderId") Long leaderId, Pageable pageable);

    List<Party> findAllByScheduleId(Long scheduleId);
}