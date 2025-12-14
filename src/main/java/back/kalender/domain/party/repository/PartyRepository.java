package back.kalender.domain.party.repository;

import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.entity.PartyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyRepository extends JpaRepository<Party, Long>, PartyRepositoryCustom {
    Page<Party> findByLeaderId(Long leaderId, Pageable pageable);

    Page<Party> findByLeaderIdAndStatus(Long leaderId, PartyStatus status, Pageable pageable);
}