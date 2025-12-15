package back.kalender.domain.party.service;

import back.kalender.domain.party.dto.request.CreatePartyRequest;
import back.kalender.domain.party.dto.request.UpdatePartyRequest;
import back.kalender.domain.party.dto.response.*;
import back.kalender.domain.party.entity.*;
import back.kalender.domain.party.mapper.PartyBuilder;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyService{

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyApplicationRepository partyApplicationRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;

    @Transactional
    public CreatePartyResponse createParty(CreatePartyRequest request, Long currentUserId) {
        Schedule schedule = scheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND));

        Party party = PartyBuilder.create(request, currentUserId);

        Party savedParty = partyRepository.save(party);

        PartyMember leader = PartyMember.createLeader(savedParty.getId(), currentUserId);
        partyMemberRepository.save(leader);

        return new CreatePartyResponse(
                savedParty.getId(),
                savedParty.getLeaderId(),
                "생성 완료"
        );
    }


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

        PartyBuilder.update(request, party);

        return new UpdatePartyResponse(
                party.getId(),
                party.getLeaderId(),
                "수정 완료"
        );
    }


    @Transactional
    public void deleteParty(Long partyId, Long currentUserId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        if (!party.isLeader(currentUserId)) {
            throw new ServiceException(ErrorCode.CANNOT_DELETE_PARTY_NOT_LEADER);
        }

        partyRepository.delete(party);
    }


    public GetPartiesResponse getParties(Pageable pageable, Long currentUserId) {
        Page<Party> partyPage = partyRepository.findAll(pageable);
        return convertToGetPartiesResponse(partyPage, currentUserId);
    }


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
        List<Party> parties = partyPage.getContent();

        List<Long> leaderIds = parties.stream()
                .map(Party::getLeaderId)
                .distinct()
                .toList();

        List<Long> scheduleIds = parties.stream()
                .map(Party::getScheduleId)
                .distinct()
                .toList();

        List<Long> partyIds = parties.stream()
                .map(Party::getId)
                .toList();

        Map<Long, User> leaderMap = userRepository.findAllById(leaderIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Map<Long, Schedule> scheduleMap = scheduleRepository.findAllById(scheduleIds).stream()
                .collect(Collectors.toMap(Schedule::getId, schedule -> schedule));

        Set<Long> appliedPartyIds = partyApplicationRepository
                .findAppliedPartyIds(partyIds, currentUserId)
                .stream()
                .collect(Collectors.toSet());

        List<GetPartiesResponse.PartyItem> partyItems = parties.stream()
                .map(party -> {
                    User leader = leaderMap.get(party.getLeaderId());
                    Schedule schedule = scheduleMap.get(party.getScheduleId());

                    if (leader == null || schedule == null) {
                        throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR);
                    }

                    boolean isMyParty = party.getLeaderId().equals(currentUserId);
                    boolean isApplied = appliedPartyIds.contains(party.getId());

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

    public GetApplicantsResponse getApplicants(Long partyId, Long currentUserId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        if (!party.isLeader(currentUserId)) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED_PARTY_LEADER);
        }

        List<PartyApplication> applications = partyApplicationRepository.findByPartyId(partyId);

        // 신청자 ID 추출 및 Batch 조회
        List<Long> applicantIds = applications.stream()
                .map(PartyApplication::getApplicantId)
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(applicantIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<GetApplicantsResponse.ApplicationInfo> applicationInfos = applications.stream()
                .map(application -> {
                    User user = userMap.get(application.getApplicantId());
                    if (user == null) {
                        throw new ServiceException(ErrorCode.USER_NOT_FOUND);
                    }
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

    public GetPartyMembersResponse getPartyMembers(Long partyId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        List<PartyMember> members = partyMemberRepository.findActiveMembers(partyId);

        List<Long> userIds = members.stream()
                .map(PartyMember::getUserId)
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<GetPartyMembersResponse.MemberInfo> memberInfos = members.stream()
                .map(member -> {
                    User user = userMap.get(member.getUserId());
                    if (user == null) {
                        throw new ServiceException(ErrorCode.USER_NOT_FOUND);
                    }
                    return new GetPartyMembersResponse.MemberInfo(
                            member.getId(),
                            member.getUserId(),
                            user.getNickname(),
                            user.getProfileImage(),
                            member.getRole()
                    );
                })
                .toList();

        return new GetPartyMembersResponse(
                partyId,
                memberInfos,
                members.size()
        );
    }


    public GetMyApplicationsResponse getMyApplications(ApplicationStatus status, Pageable pageable, Long currentUserId) {
        Page<PartyApplication> applicationPage;

        if (status != null) {
            applicationPage = partyApplicationRepository.findByApplicantIdAndStatus(
                    currentUserId, status, pageable);
        } else {
            applicationPage = partyApplicationRepository.findByApplicantId(currentUserId, pageable);
        }

        List<PartyApplication> applications = applicationPage.getContent();

        List<Long> partyIds = applications.stream()
                .map(PartyApplication::getPartyId)
                .distinct()
                .toList();

        List<Long> leaderIds = applications.stream()
                .map(PartyApplication::getLeaderId)
                .distinct()
                .toList();

        Map<Long, Party> partyMap = partyRepository.findAllById(partyIds).stream()
                .collect(Collectors.toMap(Party::getId, party -> party));

        List<Long> scheduleIds = partyMap.values().stream()
                .map(Party::getScheduleId)
                .distinct()
                .toList();

        Map<Long, Schedule> scheduleMap = scheduleRepository.findAllById(scheduleIds).stream()
                .collect(Collectors.toMap(Schedule::getId, schedule -> schedule));

        Map<Long, User> leaderMap = userRepository.findAllById(leaderIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<GetMyApplicationsResponse.ApplicationItem> applicationItems = applications.stream()
                .map(application -> {
                    Party party = partyMap.get(application.getPartyId());
                    Schedule schedule = scheduleMap.get(party.getScheduleId());
                    User leader = leaderMap.get(application.getLeaderId());

                    if (party == null || schedule == null || leader == null) {
                        throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR);
                    }

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
                            application.getStatus(),
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


    public GetMyCreatedPartiesResponse getMyCreatedParties(PartyStatus status, Pageable pageable, Long currentUserId) {
        Page<Party> partyPage;

        if (status != null) {
            partyPage = partyRepository.findByLeaderIdAndStatus(currentUserId, status, pageable);
        } else {
            partyPage = partyRepository.findByLeaderId(currentUserId, pageable);
        }

        List<Party> parties = partyPage.getContent();

        List<Long> scheduleIds = parties.stream()
                .map(Party::getScheduleId)
                .distinct()
                .toList();

        List<Long> partyIds = parties.stream()
                .map(Party::getId)
                .toList();

        Map<Long, Schedule> scheduleMap = scheduleRepository.findAllById(scheduleIds).stream()
                .collect(Collectors.toMap(Schedule::getId, schedule -> schedule));

        List<GetMyCreatedPartiesResponse.CreatedPartyItem> partyItems = parties.stream()
                .map(party -> {
                    Schedule schedule = scheduleMap.get(party.getScheduleId());
                    if (schedule == null) {
                        throw new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND);
                    }

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
                                    party.getTransportType(),
                                    party.getMaxMembers(),
                                    party.getCurrentMembers(),
                                    party.getStatus()
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