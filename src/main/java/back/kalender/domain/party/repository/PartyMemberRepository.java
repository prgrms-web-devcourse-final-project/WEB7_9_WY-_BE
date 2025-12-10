package back.kalender.domain.party.repository;

import back.kalender.domain.party.entity.PartyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {

    @Query("SELECT pm FROM PartyMember pm WHERE pm.partyId = :partyId AND pm.leftAt IS NULL ORDER BY pm.createdAt ASC")
    List<PartyMember> findActiveMembers(@Param("partyId") Long partyId);

    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END FROM PartyMember pm WHERE pm.partyId = :partyId AND pm.userId = :userId AND pm.leftAt IS NULL")
    boolean existsActiveMember(@Param("partyId") Long partyId, @Param("userId") Long userId);
}