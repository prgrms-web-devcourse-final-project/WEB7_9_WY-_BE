package back.kalender.domain.party.repository;

import back.kalender.domain.party.entity.PartyApplication;
import back.kalender.domain.party.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PartyApplicationRepositoryCustom {

    Page<PartyApplication> findByApplicantIdAndStatusWithActiveParties(
            Long applicantId,
            ApplicationStatus status,
            Pageable pageable
    );
}