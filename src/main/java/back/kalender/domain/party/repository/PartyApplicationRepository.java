package back.kalender.domain.party.repository;

import back.kalender.domain.party.entity.ApplicationStatus;
import back.kalender.domain.party.entity.PartyApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartyApplicationRepository extends JpaRepository<PartyApplication, Long> {

    Optional<PartyApplication> findById(Long applicationId);

    List<PartyApplication> findByPartyId(Long partyId);

    Page<PartyApplication> findByApplicantId(Long applicantId, Pageable pageable);

    Page<PartyApplication> findByApplicantIdAndStatus(Long applicantId, ApplicationStatus status, Pageable pageable);

    boolean existsByPartyIdAndApplicantId(Long partyId, Long applicantId);

    @Query("SELECT COUNT(pa) FROM PartyApplication pa WHERE pa.partyId = :partyId AND pa.status = 'PENDING'")
    Long countPendingApplications(@Param("partyId") Long partyId);

    @Query("SELECT COUNT(pa) FROM PartyApplication pa WHERE pa.partyId = :partyId AND pa.status = 'APPROVED'")
    Long countApprovedApplications(@Param("partyId") Long partyId);

    @Query("SELECT COUNT(pa) FROM PartyApplication pa WHERE pa.partyId = :partyId AND pa.status = 'REJECTED'")
    Long countRejectedApplications(@Param("partyId") Long partyId);

    @Query("SELECT pa FROM PartyApplication pa WHERE pa.partyId = :partyId AND pa.leaderId = :leaderId")
    List<PartyApplication> findByPartyIdAndLeaderId(@Param("partyId") Long partyId, @Param("leaderId") Long leaderId);
}