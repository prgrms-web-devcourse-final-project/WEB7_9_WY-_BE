package back.kalender.domain.party.service;

import back.kalender.domain.chat.service.ChatRoomService;
import back.kalender.domain.party.dto.request.CreatePartyRequest;
import back.kalender.domain.party.dto.request.UpdatePartyRequest;
import back.kalender.domain.party.dto.response.*;
import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.entity.PartyApplication;
import back.kalender.domain.party.entity.PartyMember;
import back.kalender.domain.party.enums.ApplicationStatus;
import back.kalender.domain.party.enums.PartyStatus;
import back.kalender.domain.party.enums.PartyType;
import back.kalender.domain.party.enums.TransportType;
import back.kalender.domain.party.mapper.PartyBuilder;
import back.kalender.domain.party.repository.PartyApplicationRepository;
import back.kalender.domain.party.repository.PartyMemberRepository;
import back.kalender.domain.party.repository.PartyRepository;
import back.kalender.domain.party.repository.PartyRepositoryCustom;
import back.kalender.domain.schedule.entity.Schedule;
import back.kalender.domain.schedule.repository.ScheduleRepository;
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyService {

    private static final int MAX_COMPLETED_PARTIES_FETCH = 1000;

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyApplicationRepository partyApplicationRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final ChatRoomService chatRoomService;

    @Transactional
    public CreatePartyResponse createParty(CreatePartyRequest request, Long currentUserId) {
        Schedule schedule = scheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND));

        Party party = PartyBuilder.create(request, currentUserId);

        Party savedParty = partyRepository.save(party);

        PartyMember leader = PartyMember.createLeader(savedParty.getId(), currentUserId);
        partyMemberRepository.save(leader);

        chatRoomService.createChatRoom(savedParty.getId(), savedParty.getPartyName());

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

        chatRoomService.closeChatRoom(partyId);

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

        if (party.isFull()) {
            throw new ServiceException(ErrorCode.PARTY_FULL);
        }

        if (!party.isRecruiting()) {
            throw new ServiceException(ErrorCode.PARTY_NOT_RECRUITING);
        }

        if (partyMemberRepository.existsByPartyIdAndUserId(partyId, currentUserId)) {
            throw new ServiceException(ErrorCode.ALREADY_JOINED_BEFORE);
        }

        if (partyApplicationRepository.existsByPartyIdAndApplicantId(partyId, currentUserId)) {
            throw new ServiceException(ErrorCode.ALREADY_APPLIED);
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

        ApplicationCounts counts = getApplicationCounts(List.of(partyId)).get(partyId);
        if (counts == null) {
            counts = new ApplicationCounts(0, 0, 0);
        }

        return new GetApplicantsResponse(
                partyId,
                applicationInfos,
                new GetApplicantsResponse.ApplicationSummary(
                        applications.size(),
                        counts.pendingCount(),
                        counts.approvedCount(),
                        counts.rejectedCount()
                )
        );
    }

    private Map<Long, ApplicationCounts> getApplicationCounts(List<Long> partyIds) {
        if (partyIds.isEmpty()) {
            return Map.of();
        }

        List<PartyApplicationRepository.ApplicationCountProjection> projections =
                partyApplicationRepository.countByPartyIdsGroupByStatus(partyIds);

        // 파티별로 그룹화
        Map<Long, Map<ApplicationStatus, Long>> groupedByParty = new HashMap<>();

        for (var projection : projections) {
            groupedByParty
                    .computeIfAbsent(projection.getPartyId(), k -> new HashMap<>())
                    .put(projection.getStatus(), projection.getCount());
        }

        // ApplicationCounts로 변환
        Map<Long, ApplicationCounts> result = new HashMap<>();

        for (var entry : groupedByParty.entrySet()) {
            Long partyId = entry.getKey();
            Map<ApplicationStatus, Long> statusCounts = entry.getValue();

            int pending = statusCounts.getOrDefault(ApplicationStatus.PENDING, 0L).intValue();
            int approved = statusCounts.getOrDefault(ApplicationStatus.APPROVED, 0L).intValue();
            int rejected = statusCounts.getOrDefault(ApplicationStatus.REJECTED, 0L).intValue();

            result.put(partyId, new ApplicationCounts(pending, approved, rejected));
        }

        return result;
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

    public GetMyApplicationsResponse getMyApplications(Pageable pageable, Long currentUserId) {
        Page<PartyApplication> applicationPage = partyApplicationRepository
                .findActiveApplicationsByApplicantId(currentUserId, pageable);

        List<PartyApplication> applications = applicationPage.getContent();

        if (applications.isEmpty()) {
            return new GetMyApplicationsResponse(List.of(), 0, 0, pageable.getPageNumber());
        }

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

                    if (schedule == null || leader == null) {
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

    public GetMyCreatedPartiesResponse getMyCreatedParties(Pageable pageable, Long currentUserId) {
        Page<Party> partyPage = partyRepository.findActivePartiesByLeaderId(currentUserId, pageable);

        List<Party> parties = partyPage.getContent();

        if (parties.isEmpty()) {
            return new GetMyCreatedPartiesResponse(List.of(), 0, 0, pageable.getPageNumber());
        }

        List<Long> scheduleIds = parties.stream()
                .map(Party::getScheduleId)
                .distinct()
                .toList();

        Map<Long, Schedule> scheduleMap = scheduleRepository.findAllById(scheduleIds).stream()
                .collect(Collectors.toMap(Schedule::getId, schedule -> schedule));

        List<Long> partyIds = parties.stream()
                .map(Party::getId)
                .toList();

        Map<Long, ApplicationCounts> countsMap = getApplicationCounts(partyIds);

        List<GetMyCreatedPartiesResponse.CreatedPartyItem> partyItems = parties.stream()
                .map(party -> {
                    Schedule schedule = scheduleMap.get(party.getScheduleId());
                    if (schedule == null) {
                        throw new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND);
                    }

                    ApplicationCounts counts = countsMap.getOrDefault(
                            party.getId(),
                            new ApplicationCounts(0, 0, 0)
                    );

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
                                    counts.pendingCount(),
                                    counts.approvedCount(),
                                    counts.rejectedCount()
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

    public GetCompletedPartiesResponse getMyCompletedParties(Pageable pageable, Long currentUserId) {
        List<PartyApplication> completedApplications = partyApplicationRepository
                .findByApplicantIdAndStatus(
                        currentUserId,
                        ApplicationStatus.COMPLETED,
                        PageRequest.of(0, MAX_COMPLETED_PARTIES_FETCH)
                ).getContent();

        List<Long> joinedPartyIds = completedApplications.stream()
                .map(PartyApplication::getPartyId)
                .distinct()
                .toList();

        Page<PartyRepositoryCustom.CompletedPartyWithType> completedPage =
                partyRepository.findCompletedPartiesByUserId(
                        currentUserId,
                        joinedPartyIds,
                        pageable
                );

        List<PartyRepositoryCustom.CompletedPartyWithType> pagedParties = completedPage.getContent();

        if (pagedParties.isEmpty()) {
            return new GetCompletedPartiesResponse(
                    List.of(),
                    0,
                    0,
                    pageable.getPageNumber()
            );
        }

        List<Long> leaderIds = pagedParties.stream()
                .map(data -> data.party().getLeaderId())
                .distinct()
                .toList();

        List<Long> scheduleIds = pagedParties.stream()
                .map(data -> data.party().getScheduleId())
                .distinct()
                .toList();

        Map<Long, User> leaderMap = userRepository.findAllById(leaderIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Map<Long, Schedule> scheduleMap = scheduleRepository.findAllById(scheduleIds).stream()
                .collect(Collectors.toMap(Schedule::getId, schedule -> schedule));

        List<GetCompletedPartiesResponse.CompletedPartyItem> partyItems = pagedParties.stream()
                .map(data -> {
                    Party party = data.party();
                    String participationType = data.participationType();

                    User leader = leaderMap.get(party.getLeaderId());
                    Schedule schedule = scheduleMap.get(party.getScheduleId());

                    if (leader == null || schedule == null) {
                        throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR);
                    }

                    return new GetCompletedPartiesResponse.CompletedPartyItem(
                            party.getId(),
                            participationType,
                            new GetCompletedPartiesResponse.EventInfo(
                                    schedule.getId(),
                                    schedule.getTitle(),
                                    schedule.getLocation(),
                                    schedule.getScheduleTime()
                            ),
                            new GetCompletedPartiesResponse.PartyDetailInfo(
                                    party.getPartyName(),
                                    party.getPartyType().getDescription(),
                                    party.getDepartureLocation(),
                                    party.getArrivalLocation(),
                                    party.getMaxMembers(),
                                    party.getCurrentMembers()
                            ),
                            new GetCompletedPartiesResponse.LeaderInfo(
                                    leader.getId(),
                                    leader.getNickname()
                            ),
                            party.getStatus(),
                            party.getUpdatedAt(),
                            party.getCreatedAt()
                    );
                })
                .toList();

        return new GetCompletedPartiesResponse(
                partyItems,
                (int) completedPage.getTotalElements(),
                completedPage.getTotalPages(),
                completedPage.getNumber()
        );
    }

    @Transactional
    public void removePartyMember(Long partyId, Long userId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        PartyMember member = partyMemberRepository
                .findByPartyIdAndUserIdAndLeftAtIsNull(partyId, userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_IN_PARTY));

        member.leave(LocalDateTime.now());
        party.decrementCurrentMembers();
    }

    @Transactional
    public void kickPartyMember(Long partyId, Long targetMemberId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        PartyMember member = partyMemberRepository
                .findByPartyIdAndUserIdAndLeftAtIsNull(partyId, targetMemberId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_IN_PARTY));

        member.kick(LocalDateTime.now());
        party.decrementCurrentMembers();
    }
}