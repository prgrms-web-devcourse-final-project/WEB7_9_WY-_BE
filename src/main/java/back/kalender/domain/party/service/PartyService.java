package back.kalender.domain.party.service;

import back.kalender.domain.party.dto.request.CreatePartyRequest;
import back.kalender.domain.party.dto.request.UpdatePartyRequest;
import back.kalender.domain.party.dto.response.*;
import org.springframework.data.domain.Pageable;

public interface PartyService {

    CreatePartyResponse createParty(CreatePartyRequest request, Long currentUserId);

    UpdatePartyResponse updateParty(Long partyId, UpdatePartyRequest request, Long currentUserId);

    void deleteParty(Long partyId, Long currentUserId);

    GetPartiesResponse getParties(Pageable pageable, Long currentUserId);

    ApplyToPartyResponse applyToParty(Long partyId, Long currentUserId);

    void cancelApplication(Long partyId, Long applicationId, Long currentUserId);

    AcceptApplicationResponse acceptApplication(Long partyId, Long applicationId, Long currentUserId);

    RejectApplicationResponse rejectApplication(Long partyId, Long applicationId, Long currentUserId);

    GetApplicantsResponse getApplicants(Long partyId, Long currentUserId);

    GetPartyMembersResponse getPartyMembers(Long partyId);

    GetMyApplicationsResponse getMyApplications(String status, Pageable pageable, Long currentUserId);

    GetMyCreatedPartiesResponse getMyCreatedParties(String status, Pageable pageable, Long currentUserId);
}