package back.kalender.domain.party.repository;

import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.enums.PartyStatus;
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

    Page<Party> findByStatusOrderByCreatedAtDesc(PartyStatus status, Pageable pageable);

    @Query("SELECT p FROM Party p WHERE p.leaderId = :leaderId " +
            "AND p.status IN ('RECRUITING', 'CLOSED') " +
            "ORDER BY p.createdAt DESC")
    Page<Party> findActivePartiesByLeaderId(
            @Param("leaderId") Long leaderId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Party p WHERE p.id = :id")
    Optional<Party> findByIdWithLock(@Param("id") Long id);

    List<Party> findByStatusIn(List<PartyStatus> statuses);
}