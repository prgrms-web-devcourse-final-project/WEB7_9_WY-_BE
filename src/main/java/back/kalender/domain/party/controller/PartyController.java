package back.kalender.domain.party.controller;

import back.kalender.domain.party.dto.request.CreatePartyRequest;
import back.kalender.domain.party.dto.request.UpdatePartyRequest;
import back.kalender.domain.party.dto.response.*;
import back.kalender.domain.party.enums.PartyType;
import back.kalender.domain.party.enums.TransportType;
import back.kalender.domain.party.service.PartyService;
import back.kalender.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Party", description = "파티 관련 API")
@RestController
@RequestMapping("/api/v1/party")
@RequiredArgsConstructor
@Validated
public class PartyController implements PartyControllerSpec{

    private final PartyService partyService;

    @PostMapping
    public ResponseEntity<CreatePartyResponse> createParty(
            @Valid @RequestBody CreatePartyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CreatePartyResponse response = partyService.createParty(request, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{partyId}")
    public ResponseEntity<UpdatePartyResponse> updateParty(
            @PathVariable Long partyId,
            @Valid @RequestBody UpdatePartyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UpdatePartyResponse response = partyService.updateParty(partyId, request, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{partyId}")
    public ResponseEntity<Void> deleteParty(
            @PathVariable Long partyId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        partyService.deleteParty(partyId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<CommonPartyResponse> getParties(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Pageable pageable = PageRequest.of(page, size);
        CommonPartyResponse response = partyService.getParties(pageable, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/schedule/{scheduleId}")
    public ResponseEntity<CommonPartyResponse> getPartiesBySchedule(
            @PathVariable Long scheduleId,
            @RequestParam(required = false) PartyType partyType,
            @RequestParam(required = false) TransportType transportType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Pageable pageable = PageRequest.of(page, size);

        CommonPartyResponse response = partyService.getPartiesBySchedule(
                scheduleId, partyType, transportType,
                pageable, userDetails.getUserId()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{partyId}/application/apply")
    public ResponseEntity<ApplyToPartyResponse> applyToParty(
            @PathVariable Long partyId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ApplyToPartyResponse response = partyService.applyToParty(partyId, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{partyId}/application/{applicationId}/cancel")
    public ResponseEntity<Void> cancelApplication(
            @PathVariable Long partyId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        partyService.cancelApplication(partyId, applicationId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{partyId}/application/{applicationId}/accept")
    public ResponseEntity<AcceptApplicationResponse> acceptApplication(
            @PathVariable Long partyId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AcceptApplicationResponse response = partyService.acceptApplication(partyId, applicationId, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{partyId}/application/{applicationId}/reject")
    public ResponseEntity<RejectApplicationResponse> rejectApplication(
            @PathVariable Long partyId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        RejectApplicationResponse response = partyService.rejectApplication(partyId, applicationId, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{partyId}/application/applicants")
    public ResponseEntity<GetApplicantsResponse> getApplicants(
            @PathVariable Long partyId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        GetApplicantsResponse response = partyService.getApplicants(partyId, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{partyId}/members")
    public ResponseEntity<GetPartyMembersResponse> getPartyMembers(
            @PathVariable Long partyId
    ) {
        GetPartyMembersResponse response = partyService.getPartyMembers(partyId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/me/party/pending")
    public ResponseEntity<CommonPartyResponse> getMyPendingApplications(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Pageable pageable = PageRequest.of(page, size);
        CommonPartyResponse response = partyService.getMyPendingApplications(pageable, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/me/party/joined")
    public ResponseEntity<CommonPartyResponse> getMyJoinedParties(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Pageable pageable = PageRequest.of(page, size);
        CommonPartyResponse response = partyService.getMyJoinedParties(pageable, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/me/party/created")
    public ResponseEntity<CommonPartyResponse> getMyCreatedParties(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Pageable pageable = PageRequest.of(page, size);
        CommonPartyResponse response = partyService.getMyCreatedParties(pageable, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/me/party/completed")
    public ResponseEntity<CommonPartyResponse> getMyCompletedParties(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Pageable pageable = PageRequest.of(page, size);
        CommonPartyResponse response = partyService.getMyCompletedParties(pageable, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{partyId}/close")
    public ResponseEntity<ClosePartyResponse> closeParty(
            @PathVariable Long partyId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ClosePartyResponse response = partyService.closeParty(partyId, userDetails.getUserId());
        return ResponseEntity.ok(response);
    }
}