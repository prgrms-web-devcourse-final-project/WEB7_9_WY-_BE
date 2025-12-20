package back.kalender.domain.party.repository;

import back.kalender.domain.party.entity.PartyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {

    @Query("SELECT pm FROM PartyMember pm WHERE pm.userId = :userId AND pm.leftAt IS NULL")
    List<PartyMember> findByUserId(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END " +
            "FROM PartyMember pm " +
            "WHERE pm.partyId = :partyId AND pm.userId = :userId")
    boolean existsByPartyIdAndUserId(@Param("partyId") Long partyId,
                                     @Param("userId") Long userId);

    @Query("SELECT pm FROM PartyMember pm " +
            "WHERE pm.partyId = :partyId AND pm.leftAt IS NULL")
    List<PartyMember> findActiveMembers(@Param("partyId") Long partyId);

    Optional<PartyMember> findByPartyIdAndUserIdAndLeftAtIsNull(Long partyId, Long userId);

    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END FROM PartyMember pm WHERE pm.partyId = :partyId AND pm.userId = :userId AND pm.leftAt IS NULL")
    boolean existsActiveMember(@Param("partyId") Long partyId, @Param("userId") Long userId);
}