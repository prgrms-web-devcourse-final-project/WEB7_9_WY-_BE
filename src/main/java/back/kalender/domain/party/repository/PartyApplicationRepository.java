package back.kalender.domain.party.repository;

import back.kalender.domain.party.enums.ApplicationStatus;
import back.kalender.domain.party.entity.PartyApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartyApplicationRepository extends JpaRepository<PartyApplication, Long>, PartyApplicationRepositoryCustom {

    List<PartyApplication> findByPartyId(Long partyId);

    boolean existsByPartyIdAndApplicantId(Long partyId, Long applicantId);

    @Query("SELECT pa.partyId FROM PartyApplication pa " +
            "WHERE pa.partyId IN :partyIds AND pa.applicantId = :applicantId")
    List<Long> findAppliedPartyIds(
            @Param("partyIds") List<Long> partyIds,
            @Param("applicantId") Long applicantId
    );

    @Query("SELECT pa.partyId as partyId, pa.status as status, COUNT(pa) as count " +
            "FROM PartyApplication pa " +
            "WHERE pa.partyId IN :partyIds " +
            "GROUP BY pa.partyId, pa.status")
    List<ApplicationCountProjection> countByPartyIdsGroupByStatus(@Param("partyIds") List<Long> partyIds);

    Page<PartyApplication> findByApplicantIdAndStatus(
            Long applicantId,
            ApplicationStatus status,
            Pageable pageable
    );


    interface ApplicationCountProjection {
        Long getPartyId();
        ApplicationStatus getStatus();
        Long getCount();
    }
}