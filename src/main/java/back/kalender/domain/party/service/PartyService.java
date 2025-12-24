package back.kalender.domain.party.service;

import back.kalender.domain.notification.enums.NotificationType;
import back.kalender.domain.notification.service.NotificationService;
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
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
    private final NotificationService notificationService;
    private final ChatRoomService chatRoomService;

    @Transactional
    public CreatePartyResponse createParty(CreatePartyRequest request, Long currentUserId) {
        log.info("[파티 생성 시작] userId={}, scheduleId={}, partyName={}",
                currentUserId, request.scheduleId(), request.partyName());

        Schedule schedule = scheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND));

        Party party = PartyBuilder.create(request, currentUserId);
        Party savedParty = partyRepository.save(party);

        PartyMember leader = PartyMember.createLeader(savedParty.getId(), currentUserId);
        partyMemberRepository.save(leader);

        chatRoomService.createChatRoom(savedParty.getId(), savedParty.getPartyName());

        log.info("[파티 생성 완료] partyId={}, leaderId={}, scheduleId={}",
                savedParty.getId(), currentUserId, request.scheduleId());

        return new CreatePartyResponse(
                savedParty.getId(),
                savedParty.getLeaderId(),
                "생성 완료"
        );
    }

    @Transactional
    public UpdatePartyResponse updateParty(Long partyId, UpdatePartyRequest request, Long currentUserId) {
        log.info("[파티 수정 시작] partyId={}, userId={}", partyId, currentUserId);

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        if (!party.isLeader(currentUserId)) {
            log.warn("[권한 없음] partyId={}, attemptUserId={}, actualLeaderId={}",
                    partyId, currentUserId, party.getLeaderId());
            throw new ServiceException(ErrorCode.CANNOT_MODIFY_PARTY_NOT_LEADER);
        }

        if (request.maxMembers() != null && request.maxMembers() < party.getCurrentMembers()) {
            log.warn("[최대 인원 축소 실패] partyId={}, requestedMax={}, currentMembers={}",
                    partyId, request.maxMembers(), party.getCurrentMembers());
            throw new ServiceException(ErrorCode.CANNOT_REDUCE_MAX_MEMBERS);
        }

        PartyBuilder.update(request, party);

        log.info("[파티 수정 완료] partyId={}, userId={}", partyId, currentUserId);

        return new UpdatePartyResponse(
                party.getId(),
                party.getLeaderId(),
                "수정 완료"
        );
    }

    @Transactional
    public void deleteParty(Long partyId, Long currentUserId) {
        log.info("[파티 삭제 시작] partyId={}, userId={}", partyId, currentUserId);

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        if (!party.isLeader(currentUserId)) {
            log.warn("[권한 없음] partyId={}, attemptUserId={}, actualLeaderId={}",
                    partyId, currentUserId, party.getLeaderId());
            throw new ServiceException(ErrorCode.CANNOT_DELETE_PARTY_NOT_LEADER);
        }

        chatRoomService.closeChatRoom(partyId);
        partyRepository.delete(party);

        log.info("[파티 삭제 완료] partyId={}, userId={}", partyId, currentUserId);
    }

    public GetPartiesResponse getParties(Pageable pageable, Long currentUserId) {
        log.debug("[파티 목록 조회] userId={}, page={}, size={}",
                currentUserId, pageable.getPageNumber(), pageable.getPageSize());

        Page<Party> partyPage = partyRepository.findAll(pageable);

        log.debug("[파티 목록 조회 완료] userId={}, totalElements={}",
                currentUserId, partyPage.getTotalElements());

        return convertToGetPartiesResponse(partyPage, currentUserId);
    }

    public GetPartiesResponse getPartiesBySchedule(
            Long scheduleId,
            PartyType partyType,
            TransportType transportType,
            Pageable pageable,
            Long currentUserId
    ) {
        log.debug("[일정별 파티 조회] scheduleId={}, partyType={}, transportType={}, userId={}",
                scheduleId, partyType, transportType, currentUserId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ServiceException(ErrorCode.SCHEDULE_NOT_FOUND));

        Page<Party> partyPage = partyRepository.findByScheduleIdWithFilters(
                scheduleId,
                partyType,
                transportType,
                pageable
        );

        log.debug("[일정별 파티 조회 완료] scheduleId={}, totalElements={}",
                scheduleId, partyPage.getTotalElements());

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
        log.info("[파티 신청 시작] partyId={}, userId={}", partyId, currentUserId);

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        if (party.isLeader(currentUserId)) {
            log.warn("[본인 파티 신청 시도] partyId={}, userId={}", partyId, currentUserId);
            throw new ServiceException(ErrorCode.CANNOT_APPLY_OWN_PARTY);
        }

        if (party.isFull()) {
            log.warn("[파티 정원 초과] partyId={}, current={}, max={}",
                    partyId, party.getCurrentMembers(), party.getMaxMembers());
            throw new ServiceException(ErrorCode.PARTY_FULL);
        }

        if (!party.isRecruiting()) {
            log.warn("[모집 중 아님] partyId={}, status={}", partyId, party.getStatus());
            throw new ServiceException(ErrorCode.PARTY_NOT_RECRUITING);
        }

        if (partyMemberRepository.existsByPartyIdAndUserId(partyId, currentUserId)) {
            log.warn("[이미 가입한 멤버] partyId={}, userId={}", partyId, currentUserId);
            throw new ServiceException(ErrorCode.ALREADY_JOINED_BEFORE);
        }

        if (partyApplicationRepository.existsByPartyIdAndApplicantId(partyId, currentUserId)) {
            log.warn("[중복 신청] partyId={}, userId={}", partyId, currentUserId);
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

        String message = String.format("%s(%d/%s)님이 '%s' 파티를 신청했습니다.",
                user.getNickname(), user.getAge(), user.getGender(), party.getPartyName());

        notificationService.send(
                party.getLeaderId(),
                NotificationType.APPLY,
                "새로운 파티 신청",
                message,
                "/party/" + partyId
        );

        log.info("[파티 신청 완료] partyId={}, userId={}, applicationId={}",
                partyId, currentUserId, application.getId());

        return new ApplyToPartyResponse(
                user.getNickname(),
                user.getAge(),
                user.getGender(),
                party.getPartyName()
        );
    }

    @Transactional
    public void cancelApplication(Long partyId, Long applicationId, Long currentUserId) {
        log.info("[신청 취소 시작] partyId={}, applicationId={}, userId={}",
                partyId, applicationId, currentUserId);

        PartyApplication application = partyApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ServiceException(ErrorCode.APPLICATION_NOT_FOUND));

        if (!application.getApplicantId().equals(currentUserId)) {
            log.warn("[권한 없음] applicationId={}, attemptUserId={}, actualApplicantId={}",
                    applicationId, currentUserId, application.getApplicantId());
            throw new ServiceException(ErrorCode.UNAUTHORIZED_PARTY_ACCESS);
        }

        if (application.isApproved()) {
            log.warn("[승인된 신청 취소 시도] applicationId={}, status={}",
                    applicationId, application.getStatus());
            throw new ServiceException(ErrorCode.CANNOT_CANCEL_APPROVED_APPLICATION);
        }

        partyApplicationRepository.delete(application);

        log.info("[신청 취소 완료] partyId={}, applicationId={}, userId={}",
                partyId, applicationId, currentUserId);
    }

    @Transactional
    public AcceptApplicationResponse acceptApplication(Long partyId, Long applicationId, Long currentUserId) {
        log.info("[신청 승인 시작] partyId={}, applicationId={}, userId={}",
                partyId, applicationId, currentUserId);

        Party party = partyRepository.findByIdWithLock(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        if (!party.isLeader(currentUserId)) {
            log.warn("[권한 없음] partyId={}, attemptUserId={}, actualLeaderId={}",
                    partyId, currentUserId, party.getLeaderId());
            throw new ServiceException(ErrorCode.UNAUTHORIZED_PARTY_LEADER);
        }

        PartyApplication application = partyApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ServiceException(ErrorCode.APPLICATION_NOT_FOUND));

        if (application.isProcessed()) {
            log.warn("[중복 처리 시도] applicationId={}, status={}",
                    applicationId, application.getStatus());
            throw new ServiceException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }

        if (party.isFull()) {
            log.warn("[파티 정원 초과] partyId={}, current={}, max={}",
                    partyId, party.getCurrentMembers(), party.getMaxMembers());
            throw new ServiceException(ErrorCode.PARTY_FULL);
        }

        application.approve();

        PartyMember member = PartyMember.createMember(partyId, application.getApplicantId());
        partyMemberRepository.save(member);

        party.incrementCurrentMembers();

        if (party.isFull()) {
            party.changeStatus(PartyStatus.CLOSED);
            log.info("[파티 정원 마감] partyId={}, members={}/{}",
                    partyId, party.getCurrentMembers(), party.getMaxMembers());
        }

        notificationService.send(
                application.getApplicantId(),
                NotificationType.ACCEPT,
                "파티 신청 수락",
                String.format("'%s' 파티 참여가 수락되었습니다.", party.getPartyName()),
                "/party/" + partyId
        );

        log.info("[신청 승인 완료] partyId={}, applicationId={}, applicantId={}, currentMembers={}",
                partyId, applicationId, application.getApplicantId(), party.getCurrentMembers());

        return new AcceptApplicationResponse(
                application.getApplicantId(),
                party.getPartyName(),
                null
        );
    }

    @Transactional
    public RejectApplicationResponse rejectApplication(Long partyId, Long applicationId, Long currentUserId) {
        log.info("[신청 거절 시작] partyId={}, applicationId={}, userId={}",
                partyId, applicationId, currentUserId);

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        if (!party.isLeader(currentUserId)) {
            log.warn("[권한 없음] partyId={}, attemptUserId={}, actualLeaderId={}",
                    partyId, currentUserId, party.getLeaderId());
            throw new ServiceException(ErrorCode.UNAUTHORIZED_PARTY_LEADER);
        }

        PartyApplication application = partyApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ServiceException(ErrorCode.APPLICATION_NOT_FOUND));

        if (application.isProcessed()) {
            log.warn("[중복 처리 시도] applicationId={}, status={}",
                    applicationId, application.getStatus());
            throw new ServiceException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }

        application.reject();

        notificationService.send(
                application.getApplicantId(),
                NotificationType.REJECT,
                "파티 신청 거절",
                String.format("'%s' 파티 참여가 거절되었습니다.", party.getPartyName()),
                "/party/" + partyId
        );

        log.info("[신청 거절 완료] partyId={}, applicationId={}, applicantId={}",
                partyId, applicationId, application.getApplicantId());

        return new RejectApplicationResponse(
                application.getApplicantId(),
                party.getPartyName(),
                null
        );
    }

    public GetApplicantsResponse getApplicants(Long partyId, Long currentUserId) {
        log.debug("[신청자 목록 조회] partyId={}, userId={}", partyId, currentUserId);

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        if (!party.isLeader(currentUserId)) {
            log.warn("[권한 없음] partyId={}, attemptUserId={}, actualLeaderId={}",
                    partyId, currentUserId, party.getLeaderId());
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

        log.debug("[신청자 목록 조회 완료] partyId={}, totalApplications={}",
                partyId, applications.size());

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

        Map<Long, Map<ApplicationStatus, Long>> groupedByParty = new HashMap<>();

        for (var projection : projections) {
            groupedByParty
                    .computeIfAbsent(projection.getPartyId(), k -> new HashMap<>())
                    .put(projection.getStatus(), projection.getCount());
        }

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
        log.debug("[멤버 목록 조회] partyId={}", partyId);

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

        log.debug("[멤버 목록 조회 완료] partyId={}, memberCount={}", partyId, members.size());

        return new GetPartyMembersResponse(
                partyId,
                memberInfos,
                members.size()
        );
    }

    public GetMyApplicationsResponse getMyApplications(Pageable pageable, Long currentUserId) {
        log.debug("[내 신청 목록 조회] userId={}, page={}", currentUserId, pageable.getPageNumber());

        Page<PartyApplication> applicationPage = partyApplicationRepository
                .findActiveApplicationsByApplicantId(currentUserId, pageable);

        List<PartyApplication> applications = applicationPage.getContent();

        if (applications.isEmpty()) {
            log.debug("[내 신청 목록 조회 완료] userId={}, totalElements=0", currentUserId);
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

        log.debug("[내 신청 목록 조회 완료] userId={}, totalElements={}",
                currentUserId, applicationPage.getTotalElements());

        return new GetMyApplicationsResponse(
                applicationItems,
                (int) applicationPage.getTotalElements(),
                applicationPage.getTotalPages(),
                applicationPage.getNumber()
        );
    }

    public GetMyCreatedPartiesResponse getMyCreatedParties(Pageable pageable, Long currentUserId) {
        log.debug("[내가 만든 파티 조회] userId={}, page={}", currentUserId, pageable.getPageNumber());

        Page<Party> partyPage = partyRepository.findActivePartiesByLeaderId(currentUserId, pageable);

        List<Party> parties = partyPage.getContent();

        if (parties.isEmpty()) {
            log.debug("[내가 만든 파티 조회 완료] userId={}, totalElements=0", currentUserId);
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

        log.debug("[내가 만든 파티 조회 완료] userId={}, totalElements={}",
                currentUserId, partyPage.getTotalElements());

        return new GetMyCreatedPartiesResponse(
                partyItems,
                (int) partyPage.getTotalElements(),
                partyPage.getTotalPages(),
                partyPage.getNumber()
        );
    }

    public GetCompletedPartiesResponse getMyCompletedParties(Pageable pageable, Long currentUserId) {
        log.debug("[종료된 파티 조회] userId={}, page={}", currentUserId, pageable.getPageNumber());

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
            log.debug("[종료된 파티 조회 완료] userId={}, totalElements=0", currentUserId);
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

        log.debug("[종료된 파티 조회 완료] userId={}, totalElements={}",
                currentUserId, completedPage.getTotalElements());

        return new GetCompletedPartiesResponse(
                partyItems,
                (int) completedPage.getTotalElements(),
                completedPage.getTotalPages(),
                completedPage.getNumber()
        );
    }

    @Transactional
    public void removePartyMember(Long partyId, Long userId) {
        log.info("[멤버 탈퇴] partyId={}, userId={}", partyId, userId);

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        PartyMember member = partyMemberRepository
                .findByPartyIdAndUserIdAndLeftAtIsNull(partyId, userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_IN_PARTY));

        member.leave(LocalDateTime.now());
        party.decrementCurrentMembers();

        log.info("[멤버 탈퇴 완료] partyId={}, userId={}, remainingMembers={}",
                partyId, userId, party.getCurrentMembers());
    }

    @Transactional
    public void kickPartyMember(Long partyId, Long targetMemberId) {
        log.info("[멤버 강퇴] partyId={}, targetMemberId={}", partyId, targetMemberId);

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        PartyMember member = partyMemberRepository
                .findByPartyIdAndUserIdAndLeftAtIsNull(partyId, targetMemberId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_IN_PARTY));

        member.kick(LocalDateTime.now());
        party.decrementCurrentMembers();

        log.info("[멤버 강퇴 완료] partyId={}, targetMemberId={}, remainingMembers={}",
                partyId, targetMemberId, party.getCurrentMembers());
    }
}