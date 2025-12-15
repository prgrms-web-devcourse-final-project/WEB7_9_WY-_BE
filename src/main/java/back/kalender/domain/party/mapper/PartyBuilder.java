package back.kalender.domain.party.mapper;

import back.kalender.domain.party.dto.request.CreatePartyRequest;
import back.kalender.domain.party.dto.request.UpdatePartyRequest;
import back.kalender.domain.party.entity.Party;

public class PartyBuilder {
    public static Party create(CreatePartyRequest request, Long currentUserId) {
        return Party.builder()
                .scheduleId(request.scheduleId())
                .leaderId(currentUserId)
                .partyType(request.partyType())
                .partyName(request.partyName())
                .description(request.description())
                .departureLocation(request.departureLocation())
                .arrivalLocation(request.arrivalLocation())
                .transportType(request.transportType())
                .maxMembers(request.maxMembers())
                .preferredGender(request.preferredGender())
                .preferredAge(request.preferredAge())
                .build();
    }

    public static Party update(UpdatePartyRequest request, Party party) {
        return party.update(
                request.partyName(),
                request.description(),
                request.departureLocation(),
                request.arrivalLocation(),
                request.transportType(),
                request.maxMembers(),
                request.preferredGender(),
                request.preferredAge()
        );
    }
}