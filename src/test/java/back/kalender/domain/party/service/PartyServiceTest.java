package back.kalender.domain.party.service;

import back.kalender.domain.chat.service.ChatRoomService;
import back.kalender.domain.notification.enums.NotificationType;
import back.kalender.domain.notification.service.NotificationService;
import back.kalender.domain.party.dto.request.CreatePartyRequest;
import back.kalender.domain.party.dto.request.UpdatePartyRequest;
import back.kalender.domain.party.dto.response.*;
import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.entity.PartyApplication;
import back.kalender.domain.party.entity.PartyMember;
import back.kalender.domain.party.enums.*;
import back.kalender.domain.party.repository.PartyApplicationRepository;
import back.kalender.domain.party.repository.PartyMemberRepository;
import back.kalender.domain.party.repository.PartyRepository;
import back.kalender.domain.party.repository.PartyRepositoryCustom;
import back.kalender.domain.schedule.entity.Schedule;
import back.kalender.domain.schedule.repository.ScheduleRepository;
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import back.kalender.global.common.enums.Gender;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartyService 테스트")
class PartyServiceTest {

    @InjectMocks
    private PartyService partyService;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private PartyMemberRepository partyMemberRepository;

    @Mock
    private PartyApplicationRepository partyApplicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private NotificationService notificationService;

    private User testUser;
    private User applicantUser;
    private Schedule testSchedule;
    private Party testParty;
    private CreatePartyRequest createRequest;

    @BeforeEach
    void setUp() throws Exception {
        testUser = User.builder()
                .email("test@example.com")
                .password("password")
                .nickname("테스터")
                .birthDate(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .build();
        setId(testUser, 1L);

        applicantUser = User.builder()
                .email("applicant@example.com")
                .password("password")
                .nickname("신청자")
                .birthDate(LocalDate.of(1995, 5, 5))
                .gender(Gender.FEMALE)
                .build();
        setId(applicantUser, 2L);

        testSchedule = Schedule.builder()
                .title("BTS 콘서트")
                .location("잠실종합운동장")
                .scheduleTime(LocalDateTime.now().plusDays(7))
                .build();
        setId(testSchedule, 1L);

        testParty = Party.builder()
                .scheduleId(1L)
                .leaderId(1L)
                .partyType(PartyType.LEAVE)
                .partyName("즐거운 파티")
                .description("같이 가요")
                .departureLocation("강남역")
                .arrivalLocation("잠실종합운동장")
                .transportType(TransportType.TAXI)
                .maxMembers(4)
                .preferredGender(Gender.ANY)
                .preferredAge(PreferredAge.ANY)
                .build();
        setId(testParty, 1L);

        createRequest = new CreatePartyRequest(
                1L,
                PartyType.LEAVE,
                "즐거운 파티",
                "같이 가요",
                "강남역",
                "잠실종합운동장",
                TransportType.TAXI,
                4,
                Gender.ANY,
                PreferredAge.ANY
        );
    }

    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Nested
    @DisplayName("파티 생성 테스트")
    class CreatePartyTest {

        @Test
        @DisplayName("성공: 파티 생성 시 PartyMember와 ChatRoom이 함께 생성된다")
        void createParty_Success() throws Exception {
            
            Long userId = 1L;
            given(scheduleRepository.existsById(1L)).willReturn(true);

            given(partyRepository.save(any(Party.class))).willAnswer(invocation -> {
                Party party = invocation.getArgument(0);
                setId(party, 1L);
                return party;
            });

            given(partyMemberRepository.save(any(PartyMember.class)))
                    .willReturn(PartyMember.createLeader(1L, userId));
            willDoNothing().given(chatRoomService).createChatRoom(anyLong(), anyString());

            
            CreatePartyResponse response = partyService.createParty(createRequest, userId);

            
            assertThat(response).isNotNull();
            assertThat(response.leaderId()).isEqualTo(userId);
            assertThat(response.status()).isEqualTo("생성 완료");

            then(scheduleRepository).should().existsById(1L);
            then(partyRepository).should().save(any(Party.class));
            then(partyMemberRepository).should().save(any(PartyMember.class));
            then(chatRoomService).should().createChatRoom(1L, "즐거운 파티");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 스케줄")
        void createParty_ScheduleNotFound() {
            
            given(scheduleRepository.existsById(1L)).willReturn(false);

            
            assertThatThrownBy(() -> partyService.createParty(createRequest, 1L))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.SCHEDULE_NOT_FOUND.getMessage());

            then(partyRepository).should(never()).save(any(Party.class));
        }
    }

    @Nested
    @DisplayName("파티 수정 테스트")
    class UpdatePartyTest {

        @Test
        @DisplayName("성공: 파티장이 파티 정보를 수정한다")
        void updateParty_Success() {
            
            Long partyId = 1L;
            Long leaderId = 1L;
            UpdatePartyRequest request = new UpdatePartyRequest(
                    "수정된 파티",
                    "수정된 설명",
                    "신림역",
                    "강남역",
                    TransportType.CARPOOL,
                    5,
                    Gender.FEMALE,
                    PreferredAge.TWENTY
            );

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));

            
            UpdatePartyResponse response = partyService.updateParty(partyId, request, leaderId);

            
            assertThat(response).isNotNull();
            assertThat(response.partyId()).isEqualTo(partyId);
            assertThat(response.status()).isEqualTo("수정 완료");

            assertThat(testParty.getPartyName()).isEqualTo("수정된 파티");
            assertThat(testParty.getDescription()).isEqualTo("수정된 설명");
            assertThat(testParty.getDepartureLocation()).isEqualTo("신림역");
            assertThat(testParty.getArrivalLocation()).isEqualTo("강남역");
            assertThat(testParty.getTransportType()).isEqualTo(TransportType.CARPOOL);
            assertThat(testParty.getMaxMembers()).isEqualTo(5);
            assertThat(testParty.getPreferredGender()).isEqualTo(Gender.FEMALE);
            assertThat(testParty.getPreferredAge()).isEqualTo(PreferredAge.TWENTY);
        }

        @Test
        @DisplayName("성공: null 필드는 수정하지 않는다")
        void updateParty_NullFieldsNotUpdated() {
            
            Long partyId = 1L;
            Long leaderId = 1L;
            String originalDescription = testParty.getDescription();
            TransportType originalTransport = testParty.getTransportType();

            UpdatePartyRequest request = new UpdatePartyRequest(
                    "새 이름",
                    null, null, null, null, null, null, null
            );

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));

            
            partyService.updateParty(partyId, request, leaderId);

            
            assertThat(testParty.getPartyName()).isEqualTo("새 이름");
            assertThat(testParty.getDescription()).isEqualTo(originalDescription);
            assertThat(testParty.getTransportType()).isEqualTo(originalTransport);
        }

        @Test
        @DisplayName("실패: 파티장이 아닌 사용자가 수정 시도")
        void updateParty_NotLeader() {
            
            Long partyId = 1L;
            Long notLeaderId = 999L;
            UpdatePartyRequest request = new UpdatePartyRequest(
                    "수정된 파티", null, null, null, null, null, null, null
            );

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));

            
            assertThatThrownBy(() -> partyService.updateParty(partyId, request, notLeaderId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.CANNOT_MODIFY_PARTY_NOT_LEADER.getMessage());
        }

        @Test
        @DisplayName("실패: 현재 인원보다 적게 최대 인원 수정")
        void updateParty_CannotReduceMaxMembers() throws Exception {
            
            Long partyId = 1L;
            Long leaderId = 1L;

            Party partyWith3Members = Party.builder()
                    .scheduleId(1L)
                    .leaderId(leaderId)
                    .partyType(PartyType.LEAVE)
                    .partyName("파티")
                    .departureLocation("강남역")
                    .arrivalLocation("잠실")
                    .transportType(TransportType.TAXI)
                    .maxMembers(4)
                    .preferredGender(Gender.ANY)
                    .preferredAge(PreferredAge.ANY)
                    .build();
            setId(partyWith3Members, 1L);

            partyWith3Members.incrementCurrentMembers();
            partyWith3Members.incrementCurrentMembers();

            UpdatePartyRequest request = new UpdatePartyRequest(
                    null, null, null, null, null,
                    2,  // 현재 3명인데 2명으로 줄이려고 시도
                    null, null
            );

            given(partyRepository.findById(partyId)).willReturn(Optional.of(partyWith3Members));

            
            assertThatThrownBy(() -> partyService.updateParty(partyId, request, leaderId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.CANNOT_REDUCE_MAX_MEMBERS.getMessage());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 파티 수정")
        void updateParty_PartyNotFound() {
            
            Long partyId = 999L;
            UpdatePartyRequest request = new UpdatePartyRequest(
                    "파티", null, null, null, null, null, null, null
            );

            given(partyRepository.findById(partyId)).willReturn(Optional.empty());

            
            assertThatThrownBy(() -> partyService.updateParty(partyId, request, 1L))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.PARTY_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("파티 모집 마감 테스트")
    class ClosePartyTest {

        @Test
        @DisplayName("성공: 파티장이 모집을 마감한다")
        void closeParty_Success() {
            
            Long partyId = 1L;
            Long leaderId = 1L;

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));

            
            ClosePartyResponse response = partyService.closeParty(partyId, leaderId);

            
            assertThat(response).isNotNull();
            assertThat(response.partyId()).isEqualTo(partyId);
            assertThat(response.message()).isEqualTo("모집이 마감되었습니다.");
            assertThat(testParty.getStatus()).isEqualTo(PartyStatus.CLOSED);
        }

        @Test
        @DisplayName("실패: 파티장이 아닌 사용자가 마감 시도")
        void closeParty_NotLeader() {
            
            Long partyId = 1L;
            Long notLeaderId = 999L;

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));

            
            assertThatThrownBy(() -> partyService.closeParty(partyId, notLeaderId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.CANNOT_MODIFY_PARTY_NOT_LEADER.getMessage());
        }

        @Test
        @DisplayName("실패: 모집 중이 아닌 파티 마감 시도")
        void closeParty_NotRecruiting() throws Exception {
            
            Long partyId = 1L;
            Long leaderId = 1L;

            Party closedParty = Party.builder()
                    .scheduleId(1L)
                    .leaderId(leaderId)
                    .partyType(PartyType.LEAVE)
                    .partyName("파티")
                    .departureLocation("강남역")
                    .arrivalLocation("잠실")
                    .transportType(TransportType.TAXI)
                    .maxMembers(4)
                    .preferredGender(Gender.ANY)
                    .preferredAge(PreferredAge.ANY)
                    .build();
            setId(closedParty, 1L);
            closedParty.changeStatus(PartyStatus.CLOSED);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(closedParty));

            
            assertThatThrownBy(() -> partyService.closeParty(partyId, leaderId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.PARTY_NOT_RECRUITING.getMessage());
        }
    }

    @Nested
    @DisplayName("파티 삭제 테스트")
    class DeletePartyTest {

        @Test
        @DisplayName("성공: 파티장이 파티와 채팅방을 삭제한다")
        void deleteParty_Success() {
            
            Long partyId = 1L;
            Long leaderId = 1L;

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            willDoNothing().given(chatRoomService).closeChatRoom(partyId);
            willDoNothing().given(partyRepository).delete(testParty);

            
            partyService.deleteParty(partyId, leaderId);

            
            then(chatRoomService).should().closeChatRoom(partyId);
            then(partyRepository).should().delete(testParty);
        }

        @Test
        @DisplayName("실패: 파티장이 아닌 사용자가 삭제 시도")
        void deleteParty_NotLeader() {
            
            Long partyId = 1L;
            Long notLeaderId = 999L;

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));

            
            assertThatThrownBy(() -> partyService.deleteParty(partyId, notLeaderId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.CANNOT_DELETE_PARTY_NOT_LEADER.getMessage());

            then(chatRoomService).should(never()).closeChatRoom(anyLong());
            then(partyRepository).should(never()).delete(any(Party.class));
        }
    }

    @Nested
    @DisplayName("파티 목록 조회 테스트")
    class GetPartiesTest {

        @Test
        @DisplayName("성공: 모집 중인 파티 목록을 조회한다 (CommonPartyResponse)")
        void getParties_Success() {
            
            Long currentUserId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            Page<Party> partyPage = new PageImpl<>(List.of(testParty), pageable, 1);

            given(partyRepository.findByStatusOrderByCreatedAtDesc(PartyStatus.RECRUITING, pageable))
                    .willReturn(partyPage);
            given(userRepository.findAllById(anyList())).willReturn(List.of(testUser));
            given(scheduleRepository.findAllById(anyList())).willReturn(List.of(testSchedule));
            given(partyApplicationRepository.findAppliedPartyIds(anyList(), eq(currentUserId)))
                    .willReturn(Collections.emptyList());

            
            CommonPartyResponse response = partyService.getParties(pageable, currentUserId);

            
            assertThat(response).isNotNull();
            assertThat(response.parties()).hasSize(1);
            assertThat(response.totalElements()).isEqualTo(1);
            assertThat(response.totalPages()).isEqualTo(1);
            assertThat(response.pageNumber()).isEqualTo(0);

            CommonPartyResponse.PartyItem item = response.parties().get(0);
            assertThat(item.partyId()).isEqualTo(testParty.getId());
            assertThat(item.schedule().title()).isEqualTo("BTS 콘서트");
            assertThat(item.leader().nickname()).isEqualTo("테스터");
            assertThat(item.isMyParty()).isTrue();
            assertThat(item.isApplied()).isFalse();
            assertThat(item.participationType()).isNull();
        }

        @Test
        @DisplayName("성공: 빈 목록 조회")
        void getParties_EmptyList() {
            
            Long currentUserId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            Page<Party> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            given(partyRepository.findByStatusOrderByCreatedAtDesc(PartyStatus.RECRUITING, pageable))
                    .willReturn(emptyPage);

            
            CommonPartyResponse response = partyService.getParties(pageable, currentUserId);

            
            assertThat(response).isNotNull();
            assertThat(response.parties()).isEmpty();
            assertThat(response.totalElements()).isZero();
            assertThat(response.totalPages()).isZero();
        }
    }

    @Nested
    @DisplayName("스케줄별 파티 조회 테스트")
    class GetPartiesByScheduleTest {

        @Test
        @DisplayName("성공: 특정 스케줄의 파티 목록을 조회한다")
        void getPartiesBySchedule_Success() {
            
            Long scheduleId = 1L;
            Long currentUserId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            Page<Party> partyPage = new PageImpl<>(List.of(testParty), pageable, 1);

            given(scheduleRepository.existsById(scheduleId)).willReturn(true);
            given(partyRepository.findByScheduleIdWithFilters(
                    scheduleId, null, null, PartyStatus.RECRUITING, pageable))
                    .willReturn(partyPage);
            given(userRepository.findAllById(anyList())).willReturn(List.of(testUser));
            given(scheduleRepository.findAllById(anyList())).willReturn(List.of(testSchedule));
            given(partyApplicationRepository.findAppliedPartyIds(anyList(), eq(currentUserId)))
                    .willReturn(Collections.emptyList());

            
            CommonPartyResponse response = partyService.getPartiesBySchedule(
                    scheduleId, null, null, pageable, currentUserId);

            
            assertThat(response).isNotNull();
            assertThat(response.parties()).hasSize(1);
            assertThat(response.parties().get(0).schedule().scheduleId()).isEqualTo(scheduleId);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 스케줄")
        void getPartiesBySchedule_ScheduleNotFound() {
            
            Long scheduleId = 999L;
            Long currentUserId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            given(scheduleRepository.existsById(scheduleId)).willReturn(false);

            
            assertThatThrownBy(() -> partyService.getPartiesBySchedule(
                    scheduleId, null, null, pageable, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.SCHEDULE_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("내가 만든 파티 조회 테스트")
    class GetMyCreatedPartiesTest {

        @Test
        @DisplayName("성공: 내가 만든 활성 파티 목록을 조회한다")
        void getMyCreatedParties_Success() {
            
            Long currentUserId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            Page<Party> partyPage = new PageImpl<>(List.of(testParty), pageable, 1);

            given(partyRepository.findActivePartiesByLeaderId(currentUserId, pageable))
                    .willReturn(partyPage);
            given(userRepository.findAllById(anyList())).willReturn(List.of(testUser));
            given(scheduleRepository.findAllById(anyList())).willReturn(List.of(testSchedule));
            given(partyApplicationRepository.findAppliedPartyIds(anyList(), eq(currentUserId)))
                    .willReturn(Collections.emptyList());

            
            CommonPartyResponse response = partyService.getMyCreatedParties(pageable, currentUserId);

            
            assertThat(response).isNotNull();
            assertThat(response.parties()).hasSize(1);

            CommonPartyResponse.PartyItem item = response.parties().get(0);
            assertThat(item.isMyParty()).isTrue();
            assertThat(item.participationType()).isEqualTo("CREATED");
        }
    }

    @Nested
    @DisplayName("신청중인 파티 조회 테스트")
    class GetMyPendingApplicationsTest {

        @Test
        @DisplayName("성공: 신청중인 파티 목록을 조회한다")
        void getMyPendingApplications_Success() {
            
            Long currentUserId = 2L;
            Pageable pageable = PageRequest.of(0, 20);

            PartyApplication application = PartyApplication.create(1L, currentUserId, 1L);
            Page<PartyApplication> applicationPage = new PageImpl<>(
                    List.of(application), pageable, 1);

            given(partyApplicationRepository.findByApplicantIdAndStatusWithActiveParties(
                    currentUserId, ApplicationStatus.PENDING, pageable))
                    .willReturn(applicationPage);
            given(partyRepository.findAllById(anyList())).willReturn(List.of(testParty));
            given(userRepository.findAllById(anyList())).willReturn(List.of(testUser));
            given(scheduleRepository.findAllById(anyList())).willReturn(List.of(testSchedule));
            given(partyApplicationRepository.findAppliedPartyIds(anyList(), eq(currentUserId)))
                    .willReturn(List.of(1L));

            
            CommonPartyResponse response = partyService.getMyPendingApplications(
                    pageable, currentUserId);

            
            assertThat(response).isNotNull();
            assertThat(response.parties()).hasSize(1);

            CommonPartyResponse.PartyItem item = response.parties().get(0);
            assertThat(item.participationType()).isEqualTo("PENDING");
            assertThat(item.isApplied()).isTrue();
        }
    }

    @Nested
    @DisplayName("참여중인 파티 조회 테스트")
    class GetMyJoinedPartiesTest {

        @Test
        @DisplayName("성공: 참여중인 파티 목록을 조회한다")
        void getMyJoinedParties_Success() {
            
            Long currentUserId = 2L;
            Pageable pageable = PageRequest.of(0, 20);

            PartyApplication application = PartyApplication.create(1L, currentUserId, 1L);
            application.approve();
            Page<PartyApplication> applicationPage = new PageImpl<>(
                    List.of(application), pageable, 1);

            given(partyApplicationRepository.findByApplicantIdAndStatusWithActiveParties(
                    currentUserId, ApplicationStatus.APPROVED, pageable))
                    .willReturn(applicationPage);
            given(partyRepository.findAllById(anyList())).willReturn(List.of(testParty));
            given(userRepository.findAllById(anyList())).willReturn(List.of(testUser));
            given(scheduleRepository.findAllById(anyList())).willReturn(List.of(testSchedule));
            given(partyApplicationRepository.findAppliedPartyIds(anyList(), eq(currentUserId)))
                    .willReturn(List.of(1L));

            
            CommonPartyResponse response = partyService.getMyJoinedParties(pageable, currentUserId);

            
            assertThat(response).isNotNull();
            assertThat(response.parties()).hasSize(1);

            CommonPartyResponse.PartyItem item = response.parties().get(0);
            assertThat(item.participationType()).isEqualTo("JOINED");
            assertThat(item.isApplied()).isTrue();
        }
    }

    @Nested
    @DisplayName("종료된 파티 조회 테스트")
    class GetMyCompletedPartiesTest {

        @Test
        @DisplayName("성공: 종료된 파티 목록을 조회한다")
        void getMyCompletedParties_Success() throws Exception {
            
            Long currentUserId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            Party completedParty = Party.builder()
                    .scheduleId(1L)
                    .leaderId(currentUserId)
                    .partyType(PartyType.LEAVE)
                    .partyName("종료된 파티")
                    .departureLocation("강남역")
                    .arrivalLocation("잠실")
                    .transportType(TransportType.TAXI)
                    .maxMembers(4)
                    .preferredGender(Gender.ANY)
                    .preferredAge(PreferredAge.ANY)
                    .build();
            setId(completedParty, 1L);
            completedParty.changeStatus(PartyStatus.COMPLETED);

            PartyRepositoryCustom.CompletedPartyWithType completedData =
                    new PartyRepositoryCustom.CompletedPartyWithType(completedParty, "CREATED");
            Page<PartyRepositoryCustom.CompletedPartyWithType> completedPage =
                    new PageImpl<>(List.of(completedData), pageable, 1);

            given(partyApplicationRepository.findByApplicantIdAndStatus(
                    eq(currentUserId), eq(ApplicationStatus.COMPLETED), any(Pageable.class)))
                    .willReturn(new PageImpl<>(Collections.emptyList()));
            given(partyRepository.findCompletedPartiesByUserId(
                    eq(currentUserId), anyList(), eq(pageable)))
                    .willReturn(completedPage);
            given(userRepository.findAllById(anyList())).willReturn(List.of(testUser));
            given(scheduleRepository.findAllById(anyList())).willReturn(List.of(testSchedule));
            given(partyApplicationRepository.findAppliedPartyIds(anyList(), eq(currentUserId)))
                    .willReturn(Collections.emptyList());

            
            CommonPartyResponse response = partyService.getMyCompletedParties(
                    pageable, currentUserId);

            
            assertThat(response).isNotNull();
            assertThat(response.parties()).hasSize(1);

            CommonPartyResponse.PartyItem item = response.parties().get(0);
            assertThat(item.participationType()).isEqualTo("CREATED");
            assertThat(item.partyDetail().status()).isEqualTo(PartyStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("파티 신청 테스트")
    class ApplyToPartyTest {

        @Test
        @DisplayName("성공: 파티에 신청하고 알림이 전송된다")
        void applyToParty_Success() {
            
            Long partyId = 1L;
            Long applicantId = 2L;

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            given(userRepository.findById(applicantId)).willReturn(Optional.of(applicantUser)); 
            given(partyMemberRepository.existsByPartyIdAndUserId(partyId, applicantId))
                    .willReturn(false);
            given(partyApplicationRepository.existsByPartyIdAndApplicantId(partyId, applicantId))
                    .willReturn(false);
            given(partyApplicationRepository.save(any(PartyApplication.class)))
                    .willReturn(PartyApplication.create(partyId, applicantId, 1L));

            
            ApplyToPartyResponse response = partyService.applyToParty(partyId, applicantId);

            
            assertThat(response).isNotNull();
            assertThat(response.applicantNickName()).isEqualTo("신청자");
            assertThat(response.partyTitle()).isEqualTo("즐거운 파티");

            then(partyApplicationRepository).should().save(any(PartyApplication.class));
            then(notificationService).should().send(
                    eq(testParty.getLeaderId()),
                    eq(NotificationType.APPLY),
                    eq("새로운 파티 신청"),
                    anyString()
            );
        }

        @Test
        @DisplayName("실패: 파티장이 자신의 파티에 신청")
        void applyToParty_CannotApplyOwnParty() {
            
            Long partyId = 1L;
            Long leaderId = 1L;

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            given(userRepository.findById(leaderId)).willReturn(Optional.of(testUser));

            
            assertThatThrownBy(() -> partyService.applyToParty(partyId, leaderId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.CANNOT_APPLY_OWN_PARTY.getMessage());
        }

        @Test
        @DisplayName("실패: 인원이 꽉 찬 파티에 신청")
        void applyToParty_PartyFull() throws Exception {
            
            Long partyId = 1L;
            Long applicantId = 2L;

            Party fullParty = Party.builder()
                    .scheduleId(1L)
                    .leaderId(1L)
                    .partyType(PartyType.LEAVE)
                    .partyName("파티")
                    .departureLocation("강남역")
                    .arrivalLocation("잠실")
                    .transportType(TransportType.TAXI)
                    .maxMembers(2)
                    .preferredGender(Gender.ANY)
                    .preferredAge(PreferredAge.ANY)
                    .build();
            setId(fullParty, 1L);
            fullParty.incrementCurrentMembers();

            given(partyRepository.findById(partyId)).willReturn(Optional.of(fullParty));
            given(userRepository.findById(applicantId)).willReturn(Optional.of(applicantUser));

            
            assertThatThrownBy(() -> partyService.applyToParty(partyId, applicantId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.PARTY_FULL.getMessage());
        }

        @Test
        @DisplayName("실패: 모집 중단된 파티에 신청")
        void applyToParty_NotRecruiting() throws Exception {
            
            Long partyId = 1L;
            Long applicantId = 2L;

            Party closedParty = Party.builder()
                    .scheduleId(1L)
                    .leaderId(1L)
                    .partyType(PartyType.LEAVE)
                    .partyName("파티")
                    .departureLocation("강남역")
                    .arrivalLocation("잠실")
                    .transportType(TransportType.TAXI)
                    .maxMembers(4)
                    .preferredGender(Gender.ANY)
                    .preferredAge(PreferredAge.ANY)
                    .build();
            setId(closedParty, 1L);
            closedParty.changeStatus(PartyStatus.CLOSED);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(closedParty));
            given(userRepository.findById(applicantId)).willReturn(Optional.of(applicantUser)); 

            
            assertThatThrownBy(() -> partyService.applyToParty(partyId, applicantId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.PARTY_NOT_RECRUITING.getMessage());
        }

        @Test
        @DisplayName("실패: 이미 멤버인 사용자가 신청")
        void applyToParty_AlreadyMember() {
            
            Long partyId = 1L;
            Long memberId = 2L;

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            given(userRepository.findById(memberId)).willReturn(Optional.of(applicantUser)); 
            given(partyMemberRepository.existsByPartyIdAndUserId(partyId, memberId))
                    .willReturn(true);

            
            assertThatThrownBy(() -> partyService.applyToParty(partyId, memberId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.ALREADY_JOINED_BEFORE.getMessage());
        }

        @Test
        @DisplayName("실패: 이미 신청한 파티에 재신청")
        void applyToParty_AlreadyApplied() {
            
            Long partyId = 1L;
            Long applicantId = 2L;

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            given(userRepository.findById(applicantId)).willReturn(Optional.of(applicantUser)); 
            given(partyMemberRepository.existsByPartyIdAndUserId(partyId, applicantId))
                    .willReturn(false);
            given(partyApplicationRepository.existsByPartyIdAndApplicantId(partyId, applicantId))
                    .willReturn(true);

            
            assertThatThrownBy(() -> partyService.applyToParty(partyId, applicantId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.ALREADY_APPLIED.getMessage());
        }
    }

    @Nested
    @DisplayName("신청 승인 테스트")
    class AcceptApplicationTest {

        @Test
        @DisplayName("성공: 파티장이 신청을 승인하고 알림이 전송된다")
        void acceptApplication_Success() {
            
            Long partyId = 1L;
            Long applicationId = 1L;
            Long leaderId = 1L;
            Long applicantId = 2L;

            PartyApplication application = PartyApplication.create(partyId, applicantId, leaderId);

            given(partyRepository.findByIdWithLock(partyId)).willReturn(Optional.of(testParty));
            given(partyApplicationRepository.findById(applicationId))
                    .willReturn(Optional.of(application));
            given(partyMemberRepository.save(any(PartyMember.class)))
                    .willReturn(PartyMember.createMember(partyId, applicantId));

            
            AcceptApplicationResponse response = partyService.acceptApplication(
                    partyId, applicationId, leaderId);

            
            assertThat(response).isNotNull();
            assertThat(response.applicantId()).isEqualTo(applicantId);
            assertThat(response.partyTitle()).isEqualTo("즐거운 파티");

            then(partyMemberRepository).should().save(any(PartyMember.class));
            assertThat(testParty.getCurrentMembers()).isEqualTo(2);

            then(notificationService).should().send(
                    eq(applicantId),
                    eq(NotificationType.ACCEPT),
                    eq("파티 신청 수락"),
                    anyString()
            );
        }

        @Test
        @DisplayName("성공: 마지막 멤버 승인 시 파티 상태가 CLOSED로 변경된다")
        void acceptApplication_PartyFullAfterAccept() throws Exception {
            
            Long partyId = 1L;
            Long applicationId = 1L;
            Long leaderId = 1L;
            Long applicantId = 2L;

            Party smallParty = Party.builder()
                    .scheduleId(1L)
                    .leaderId(leaderId)
                    .partyType(PartyType.LEAVE)
                    .partyName("파티")
                    .departureLocation("강남역")
                    .arrivalLocation("잠실")
                    .transportType(TransportType.TAXI)
                    .maxMembers(2)
                    .preferredGender(Gender.ANY)
                    .preferredAge(PreferredAge.ANY)
                    .build();
            setId(smallParty, 1L);

            PartyApplication application = PartyApplication.create(partyId, applicantId, leaderId);

            given(partyRepository.findByIdWithLock(partyId)).willReturn(Optional.of(smallParty));
            given(partyApplicationRepository.findById(applicationId))
                    .willReturn(Optional.of(application));
            given(partyMemberRepository.save(any(PartyMember.class)))
                    .willReturn(PartyMember.createMember(partyId, applicantId));

            
            partyService.acceptApplication(partyId, applicationId, leaderId);

            
            assertThat(smallParty.getCurrentMembers()).isEqualTo(2);
            assertThat(smallParty.getStatus()).isEqualTo(PartyStatus.CLOSED);
        }

        @Test
        @DisplayName("실패: 이미 꽉 찬 파티에 신청 승인 시도")
        void acceptApplication_PartyAlreadyFull() throws Exception {
            
            Long partyId = 1L;
            Long applicationId = 1L;
            Long leaderId = 1L;
            Long applicantId = 2L;

            Party fullParty = Party.builder()
                    .scheduleId(1L)
                    .leaderId(leaderId)
                    .partyType(PartyType.LEAVE)
                    .partyName("파티")
                    .departureLocation("강남역")
                    .arrivalLocation("잠실")
                    .transportType(TransportType.TAXI)
                    .maxMembers(2)
                    .preferredGender(Gender.ANY)
                    .preferredAge(PreferredAge.ANY)
                    .build();
            setId(fullParty, 1L);
            fullParty.incrementCurrentMembers();

            PartyApplication application = PartyApplication.create(partyId, applicantId, leaderId);

            given(partyRepository.findByIdWithLock(partyId)).willReturn(Optional.of(fullParty));
            given(partyApplicationRepository.findById(applicationId))
                    .willReturn(Optional.of(application));

            
            assertThatThrownBy(() -> partyService.acceptApplication(
                    partyId, applicationId, leaderId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.PARTY_FULL.getMessage());
        }

        @Test
        @DisplayName("실패: 파티장이 아닌 사용자가 승인 시도")
        void acceptApplication_NotLeader() {
            
            Long partyId = 1L;
            Long applicationId = 1L;
            Long notLeaderId = 999L;

            given(partyRepository.findByIdWithLock(partyId)).willReturn(Optional.of(testParty));

            
            assertThatThrownBy(() -> partyService.acceptApplication(
                    partyId, applicationId, notLeaderId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.UNAUTHORIZED_PARTY_LEADER.getMessage());
        }

        @Test
        @DisplayName("실패: 이미 처리된 신청을 승인 시도")
        void acceptApplication_AlreadyProcessed() {
            
            Long partyId = 1L;
            Long applicationId = 1L;
            Long leaderId = 1L;
            Long applicantId = 2L;

            PartyApplication application = PartyApplication.create(partyId, applicantId, leaderId);
            application.approve();

            given(partyRepository.findByIdWithLock(partyId)).willReturn(Optional.of(testParty));
            given(partyApplicationRepository.findById(applicationId))
                    .willReturn(Optional.of(application));

            
            assertThatThrownBy(() -> partyService.acceptApplication(
                    partyId, applicationId, leaderId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.APPLICATION_ALREADY_PROCESSED.getMessage());
        }
    }

    @Nested
    @DisplayName("신청 거절 테스트")
    class RejectApplicationTest {

        @Test
        @DisplayName("성공: 파티장이 신청을 거절하고 알림이 전송된다")
        void rejectApplication_Success() {
            
            Long partyId = 1L;
            Long applicationId = 1L;
            Long leaderId = 1L;
            Long applicantId = 2L;

            PartyApplication application = PartyApplication.create(partyId, applicantId, leaderId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            given(partyApplicationRepository.findById(applicationId))
                    .willReturn(Optional.of(application));

            
            RejectApplicationResponse response = partyService.rejectApplication(
                    partyId, applicationId, leaderId);

            
            assertThat(response).isNotNull();
            assertThat(response.applicantId()).isEqualTo(applicantId);
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);

            then(notificationService).should().send(
                    eq(applicantId),
                    eq(NotificationType.REJECT),
                    eq("파티 신청 거절"),
                    anyString()
            );
        }

        @Test
        @DisplayName("실패: 파티장이 아닌 사용자가 거절 시도")
        void rejectApplication_NotLeader() {
            
            Long partyId = 1L;
            Long applicationId = 1L;
            Long notLeaderId = 999L;

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));

            
            assertThatThrownBy(() -> partyService.rejectApplication(
                    partyId, applicationId, notLeaderId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.UNAUTHORIZED_PARTY_LEADER.getMessage());
        }
    }

    @Nested
    @DisplayName("신청 취소 테스트")
    class CancelApplicationTest {

        @Test
        @DisplayName("성공: 신청자가 자신의 신청을 취소한다")
        void cancelApplication_Success() {
            
            Long partyId = 1L;
            Long applicationId = 1L;
            Long applicantId = 2L;

            PartyApplication application = PartyApplication.create(partyId, applicantId, 1L);

            given(partyApplicationRepository.findById(applicationId))
                    .willReturn(Optional.of(application));
            willDoNothing().given(partyApplicationRepository).delete(application);

            
            partyService.cancelApplication(partyId, applicationId, applicantId);

            
            then(partyApplicationRepository).should().delete(application);
        }

        @Test
        @DisplayName("실패: 다른 사용자의 신청을 취소 시도")
        void cancelApplication_Unauthorized() {
            
            Long partyId = 1L;
            Long applicationId = 1L;
            Long applicantId = 2L;
            Long otherUserId = 999L;

            PartyApplication application = PartyApplication.create(partyId, applicantId, 1L);

            given(partyApplicationRepository.findById(applicationId))
                    .willReturn(Optional.of(application));

            
            assertThatThrownBy(() -> partyService.cancelApplication(
                    partyId, applicationId, otherUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.UNAUTHORIZED_PARTY_ACCESS.getMessage());
        }

        @Test
        @DisplayName("실패: 승인된 신청을 취소 시도")
        void cancelApplication_AlreadyApproved() {
            
            Long partyId = 1L;
            Long applicationId = 1L;
            Long applicantId = 2L;

            PartyApplication application = PartyApplication.create(partyId, applicantId, 1L);
            application.approve();

            given(partyApplicationRepository.findById(applicationId))
                    .willReturn(Optional.of(application));

            
            assertThatThrownBy(() -> partyService.cancelApplication(
                    partyId, applicationId, applicantId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.CANNOT_CANCEL_APPROVED_APPLICATION.getMessage());
        }
    }

    @Nested
    @DisplayName("파티 멤버 제거 테스트")
    class RemovePartyMemberTest {

        @Test
        @DisplayName("성공: 파티 멤버를 제거하고 인원을 감소시킨다")
        void removePartyMember_Success() {
            
            Long partyId = 1L;
            Long userId = 2L;

            PartyMember member = PartyMember.createMember(partyId, userId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            given(partyMemberRepository.findByPartyIdAndUserIdAndLeftAtIsNull(partyId, userId))
                    .willReturn(Optional.of(member));

            testParty.incrementCurrentMembers();
            int beforeCount = testParty.getCurrentMembers();

            
            partyService.removePartyMember(partyId, userId);

            
            assertThat(member.getLeftAt()).isNotNull();
            assertThat(testParty.getCurrentMembers()).isEqualTo(beforeCount - 1);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 멤버 제거 시도")
        void removePartyMember_MemberNotFound() {
            
            Long partyId = 1L;
            Long userId = 999L;

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            given(partyMemberRepository.findByPartyIdAndUserIdAndLeftAtIsNull(partyId, userId))
                    .willReturn(Optional.empty());

            
            assertThatThrownBy(() -> partyService.removePartyMember(partyId, userId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.USER_NOT_IN_PARTY.getMessage());
        }
    }

    @Nested
    @DisplayName("파티 멤버 강퇴 테스트")
    class KickPartyMemberTest {

        @Test
        @DisplayName("성공: 파티 멤버를 강퇴하고 인원을 감소시킨다")
        void kickPartyMember_Success() {
            
            Long partyId = 1L;
            Long targetMemberId = 2L;

            PartyMember member = PartyMember.createMember(partyId, targetMemberId);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            given(partyMemberRepository.findByPartyIdAndUserIdAndLeftAtIsNull(
                    partyId, targetMemberId))
                    .willReturn(Optional.of(member));

            testParty.incrementCurrentMembers();
            int beforeCount = testParty.getCurrentMembers();

            
            partyService.kickPartyMember(partyId, targetMemberId);

            assertThat(member.getKickedAt()).isNotNull();
            assertThat(testParty.getCurrentMembers()).isEqualTo(beforeCount - 1);

            then(partyMemberRepository).should().findByPartyIdAndUserIdAndLeftAtIsNull(
                    partyId, targetMemberId);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 멤버 강퇴 시도")
        void kickPartyMember_MemberNotFound() {
            
            Long partyId = 1L;
            Long targetMemberId = 999L;

            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            given(partyMemberRepository.findByPartyIdAndUserIdAndLeftAtIsNull(
                    partyId, targetMemberId))
                    .willReturn(Optional.empty());

            
            assertThatThrownBy(() -> partyService.kickPartyMember(partyId, targetMemberId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.USER_NOT_IN_PARTY.getMessage());
        }
    }
}