package back.kalender.domain.party.repository;

import back.kalender.domain.party.entity.PartyApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PartyApplicationRepositoryCustom {

    Page<PartyApplication> findActiveApplicationsByApplicantId(Long applicantId, Pageable pageable);
}