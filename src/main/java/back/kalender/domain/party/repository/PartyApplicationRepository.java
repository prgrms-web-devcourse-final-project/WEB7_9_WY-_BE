package back.kalender.domain.party.repository;

import back.kalender.domain.party.entity.PartyApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartyApplicationRepository extends JpaRepository<PartyApplication, Long> {
}
