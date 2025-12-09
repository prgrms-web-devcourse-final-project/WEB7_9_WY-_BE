package back.kalender.domain.party.repository;

import back.kalender.domain.party.entity.PartyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {
}
