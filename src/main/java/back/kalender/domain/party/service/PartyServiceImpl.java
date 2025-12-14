package back.kalender.domain.party.service;

import back.kalender.domain.party.dto.request.CreatePartyRequest;
import back.kalender.domain.party.dto.request.UpdatePartyRequest;
import back.kalender.domain.party.dto.response.*;
import back.kalender.domain.party.entity.*;
import back.kalender.domain.party.repository.PartyApplicationRepository;
import back.kalender.domain.party.repository.PartyMemberRepository;
import back.kalender.domain.party.repository.PartyRepository;
import back.kalender.domain.schedule.entity.Schedule;
import back.kalender.domain.schedule.repository.ScheduleRepository;
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyServiceImpl implements PartyService {

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyApplicationRepository partyApplicationRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;

    @Override
    @Transactional
    public CreatePartyResponse createParty(CreatePartyRequest request, Long currentUserId) {
        Schedule schedule = scheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND));

        Party party = Party.builder()
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

        Party savedParty = partyRepository.save(party);

        PartyMember leader = PartyMember.createLeader(savedParty.getId(), currentUserId);
        partyMemberRepository.save(leader);

        return new CreatePartyResponse(
                savedParty.getId(),
                savedParty.getLeaderId(),
                "생성 완료"
        );
    }

    @Override
    @Transactional
    public UpdatePartyResponse updateParty(Long partyId, UpdatePartyRequest request, Long currentUserId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        if (!party.isLeader(currentUserId)) {
            throw new ServiceException(ErrorCode.CANNOT_MODIFY_PARTY_NOT_LEADER);
        }

        if (request.maxMembers() != null && request.maxMembers() < party.getCurrentMembers()) {
            throw new ServiceException(ErrorCode.CANNOT_REDUCE_MAX_MEMBERS);
        }

        party.update(
                request.partyName() != null ? request.partyName() : party.getPartyName(),
                request.description() != null ? request.description() : party.getDescription(),
                request.departureLocation() != null ? request.departureLocation() : party.getDepartureLocation(),
                request.arrivalLocation() != null ? request.arrivalLocation() : party.getArrivalLocation(),
                request.transportType() != null ? request.transportType() : party.getTransportType(),
                request.maxMembers() != null ? request.maxMembers() : party.getMaxMembers(),
                request.preferredGender() != null ? request.preferredGender() : party.getPreferredGender(),
                request.preferredAge() != null ? request.preferredAge() : party.getPreferredAge()
        );

        return new UpdatePartyResponse(
                party.getId(),
                party.getLeaderId(),
                "수정 완료"
        );
    }

    @Override
    @Transactional
    public void deleteParty(Long partyId, Long currentUserId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        if (!party.isLeader(currentUserId)) {
            throw new ServiceException(ErrorCode.CANNOT_DELETE_PARTY_NOT_LEADER);
        }

        partyRepository.delete(party);
    }

    @Override
    public GetPartiesResponse getParties(Pageable pageable, Long currentUserId) {
        Page<Party> partyPage = partyRepository.findAll(pageable);
        return convertToGetPartiesResponse(partyPage, currentUserId);
    }

    @Override
    public GetPartiesResponse getPartiesBySchedule(
            Long scheduleId,
            PartyType partyType,
            TransportType transportType,
            Pageable pageable,
            Long currentUserId
    ) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND));

        Page<Party> partyPage = partyRepository.findByScheduleIdWithFilters(
                scheduleId,
                partyType,
                transportType,
                pageable
        );

        return convertToGetPartiesResponse(partyPage, currentUserId);
    }


    private GetPartiesResponse convertToGetPartiesResponse(Page<Party> partyPage, Long currentUserId) {
        List<GetPartiesResponse.PartyItem> partyItems = partyPage.getContent().stream()
                .map(party -> {
                    User leader = userRepository.findById(party.getLeaderId())
                            .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
                    Schedule schedule = scheduleRepository.findById(party.getScheduleId())
                            .orElseThrow(() -> new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND));

                    boolean isMyParty = party.getLeaderId().equals(currentUserId);
                    boolean isApplied = partyApplicationRepository
                            .existsByPartyIdAndApplicantId(party.getId(), currentUserId);

                    return new GetPartiesResponse.PartyItem(
                            party.getId(),
                            new GetPartiesResponse.Leader(
                                    party.getLeaderId(),
                                    leader.getNickname(),
                                    leader.getAge(),
                                    leader.getGender(),
                                    leader.getProfileImage()
                            ),
                            new GetPartiesResponse.Event(
                                    party.getScheduleId(),
                                    schedule.getTitle(),
                                    schedule.getLocation()
                            ),
                            new GetPartiesResponse.PartyInfo(
                                    party.getPartyType(),
                                    party.getPartyName(),
                                    party.getDepartureLocation(),
                                    party.getArrivalLocation(),
                                    party.getTransportType(),
                                    party.getMaxMembers(),
                                    party.getCurrentMembers(),
                                    party.getDescription(),
                                    party.getStatus()
                            ),
                            isMyParty,
                            isApplied
                    );
                })
                .toList();

        return new GetPartiesResponse(
                partyItems,
                (int) partyPage.getTotalElements(),
                partyPage.getTotalPages(),
                partyPage.getNumber()
        );
    }

    @Override
    @Transactional
    public ApplyToPartyResponse applyToParty(Long partyId, Long currentUserId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        if (party.isLeader(currentUserId)) {
            throw new ServiceException(ErrorCode.CANNOT_APPLY_OWN_PARTY);
        }

        if (partyApplicationRepository.existsByPartyIdAndApplicantId(partyId, currentUserId)) {
            throw new ServiceException(ErrorCode.ALREADY_APPLIED);
        }

        if (partyMemberRepository.existsActiveMember(partyId, currentUserId)) {
            throw new ServiceException(ErrorCode.ALREADY_MEMBER);
        }

        if (party.isFull()) {
            throw new ServiceException(ErrorCode.PARTY_FULL);
        }

        if (!party.isRecruiting()) {
            throw new ServiceException(ErrorCode.PARTY_NOT_RECRUITING);
        }

        PartyApplication application = PartyApplication.create(
                partyId,
                currentUserId,
                party.getLeaderId()
        );
        partyApplicationRepository.save(application);

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        return new ApplyToPartyResponse(
                user.getNickname(),
                user.getAge(),
                user.getGender(),
                party.getPartyName()
        );
    }

    @Override
    @Transactional
    public void cancelApplication(Long partyId, Long applicationId, Long currentUserId) {
        PartyApplication application = partyApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ServiceException(ErrorCode.APPLICATION_NOT_FOUND));

        if (!application.getApplicantId().equals(currentUserId)) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED_PARTY_ACCESS);
        }

        if (application.isApproved()) {
            throw new ServiceException(ErrorCode.CANNOT_CANCEL_APPROVED_APPLICATION);
        }

        partyApplicationRepository.delete(application);
    }

    @Override
    @Transactional
    public AcceptApplicationResponse acceptApplication(Long partyId, Long applicationId, Long currentUserId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        if (!party.isLeader(currentUserId)) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED_PARTY_LEADER);
        }

        PartyApplication application = partyApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ServiceException(ErrorCode.APPLICATION_NOT_FOUND));

        if (application.isProcessed()) {
            throw new ServiceException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }

        if (party.isFull()) {
            throw new ServiceException(ErrorCode.PARTY_FULL);
        }

        application.approve();

        PartyMember member = PartyMember.createMember(partyId, application.getApplicantId());
        partyMemberRepository.save(member);

        party.incrementCurrentMembers();

        if (party.isFull()) {
            party.changeStatus(PartyStatus.CLOSED);
        }

        return new AcceptApplicationResponse(
                application.getApplicantId(),
                party.getPartyName(),
                null
        );
    }

    @Override
    @Transactional
    public RejectApplicationResponse rejectApplication(Long partyId, Long applicationId, Long currentUserId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        if (!party.isLeader(currentUserId)) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED_PARTY_LEADER);
        }

        PartyApplication application = partyApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ServiceException(ErrorCode.APPLICATION_NOT_FOUND));

        if (application.isProcessed()) {
            throw new ServiceException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }

        application.reject();

        return new RejectApplicationResponse(
                application.getApplicantId(),
                party.getPartyName(),
                null
        );
    }

    @Override
    public GetApplicantsResponse getApplicants(Long partyId, Long currentUserId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        if (!party.isLeader(currentUserId)) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED_PARTY_LEADER);
        }

        List<PartyApplication> applications = partyApplicationRepository.findByPartyId(partyId);

        List<GetApplicantsResponse.ApplicationInfo> applicationInfos = applications.stream()
                .map(application -> {
                    User user = userRepository.findById(application.getApplicantId())
                            .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
                    return new GetApplicantsResponse.ApplicationInfo(
                            application.getId(),
                            new GetApplicantsResponse.ApplicantInfo(
                                    application.getApplicantId(),
                                    user.getNickname(),
                                    user.getProfileImage(),
                                    user.getGender(),
                                    user.getAge()
                            ),
                            application.getStatus()
                    );
                })
                .toList();

        Long pendingCount = partyApplicationRepository.countPendingApplications(partyId);
        Long approvedCount = partyApplicationRepository.countApprovedApplications(partyId);
        Long rejectedCount = partyApplicationRepository.countRejectedApplications(partyId);

        return new GetApplicantsResponse(
                partyId,
                applicationInfos,
                new GetApplicantsResponse.ApplicationSummary(
                        applications.size(),
                        pendingCount.intValue(),
                        approvedCount.intValue(),
                        rejectedCount.intValue()
                )
        );
    }

    @Override
    public GetPartyMembersResponse getPartyMembers(Long partyId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        List<PartyMember> members = partyMemberRepository.findActiveMembers(partyId);

        List<GetPartyMembersResponse.MemberInfo> memberInfos = members.stream()
                .map(member -> {
                    User user = userRepository.findById(member.getUserId())
                            .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
                    return new GetPartyMembersResponse.MemberInfo(
                            member.getId(),
                            member.getUserId(),
                            user.getNickname(),
                            user.getProfileImage(),
                            member.getRole().getDescription()
                    );
                })
                .toList();

        return new GetPartyMembersResponse(
                partyId,
                memberInfos,
                members.size()
        );
    }

    @Override
    public GetMyApplicationsResponse getMyApplications(String status, Pageable pageable, Long currentUserId) {
        Page<PartyApplication> applicationPage;

        if (status != null && !status.isBlank()) {
            ApplicationStatus applicationStatus = ApplicationStatus.valueOf(status.toUpperCase());
            applicationPage = partyApplicationRepository.findByApplicantIdAndStatus(
                    currentUserId, applicationStatus, pageable);
        } else {
            applicationPage = partyApplicationRepository.findByApplicantId(currentUserId, pageable);
        }

        List<GetMyApplicationsResponse.ApplicationItem> applicationItems = applicationPage.getContent().stream()
                .map(application -> {
                    Party party = partyRepository.findById(application.getPartyId())
                            .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));
                    Schedule schedule = scheduleRepository.findById(party.getScheduleId())
                            .orElseThrow(() -> new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND));
                    User leader = userRepository.findById(application.getLeaderId())
                            .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

                    return new GetMyApplicationsResponse.ApplicationItem(
                            application.getId(),
                            new GetMyApplicationsResponse.PartyInfo(
                                    application.getPartyId(),
                                    new GetMyApplicationsResponse.LeaderInfo(
                                            application.getLeaderId(),
                                            leader.getNickname(),
                                            leader.getProfileImage()
                                    ),
                                    new GetMyApplicationsResponse.EventInfo(
                                            schedule.getId(),
                                            schedule.getTitle(),
                                            schedule.getLocation(),
                                            schedule.getScheduleTime()
                                    ),
                                    new GetMyApplicationsResponse.PartyDetailInfo(
                                            party.getPartyType().getDescription(),
                                            party.getDepartureLocation(),
                                            party.getCurrentMembers(),
                                            party.getMaxMembers()
                                    )
                            ),
                            application.getStatus().getDescription(),
                            null
                    );
                })
                .toList();

        return new GetMyApplicationsResponse(
                applicationItems,
                (int) applicationPage.getTotalElements(),
                applicationPage.getTotalPages(),
                applicationPage.getNumber()
        );
    }

    @Override
    public GetMyCreatedPartiesResponse getMyCreatedParties(String status, Pageable pageable, Long currentUserId) {
        Page<Party> partyPage;

        if (status != null && !status.isBlank()) {
            PartyStatus partyStatus = PartyStatus.valueOf(status.toUpperCase());
            partyPage = partyRepository.findByLeaderIdAndStatus(currentUserId, partyStatus, pageable);
        } else {
            partyPage = partyRepository.findByLeaderId(currentUserId, pageable);
        }

        List<GetMyCreatedPartiesResponse.CreatedPartyItem> partyItems = partyPage.getContent().stream()
                .map(party -> {
                    Schedule schedule = scheduleRepository.findById(party.getScheduleId())
                            .orElseThrow(() -> new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND));

                    Long pendingCount = partyApplicationRepository.countPendingApplications(party.getId());
                    Long approvedCount = partyApplicationRepository.countApprovedApplications(party.getId());
                    Long rejectedCount = partyApplicationRepository.countRejectedApplications(party.getId());

                    return new GetMyCreatedPartiesResponse.CreatedPartyItem(
                            party.getId(),
                            new GetMyCreatedPartiesResponse.EventInfo(
                                    schedule.getId(),
                                    schedule.getTitle(),
                                    schedule.getLocation(),
                                    schedule.getScheduleTime()
                            ),
                            new GetMyCreatedPartiesResponse.PartyDetailInfo(
                                    party.getPartyType().getDescription(),
                                    party.getDepartureLocation(),
                                    party.getArrivalLocation(),
                                    party.getTransportType().getDescription(),
                                    party.getMaxMembers(),
                                    party.getCurrentMembers(),
                                    party.getStatus().getDescription()
                            ),
                            new GetMyCreatedPartiesResponse.ApplicationStatistics(
                                    pendingCount.intValue(),
                                    approvedCount.intValue(),
                                    rejectedCount.intValue()
                            ),
                            party.getDescription(),
                            null,
                            party.getCreatedAt()
                    );
                })
                .toList();

        return new GetMyCreatedPartiesResponse(
                partyItems,
                (int) partyPage.getTotalElements(),
                partyPage.getTotalPages(),
                partyPage.getNumber()
        );
    }
}