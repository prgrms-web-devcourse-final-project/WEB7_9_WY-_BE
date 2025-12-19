//package back.kalender.domain.party.service;
//
//import back.kalender.domain.party.dto.request.CreatePartyRequest;
//import back.kalender.domain.party.dto.request.UpdatePartyRequest;
//import back.kalender.domain.party.dto.response.*;
//import back.kalender.domain.party.entity.*;
//import back.kalender.domain.party.enums.PartyStatus;
//import back.kalender.domain.party.enums.PartyType;
//import back.kalender.domain.party.enums.PreferredAge;
//import back.kalender.domain.party.enums.TransportType;
//import back.kalender.domain.party.repository.PartyApplicationRepository;
//import back.kalender.domain.party.repository.PartyMemberRepository;
//import back.kalender.domain.party.repository.PartyRepository;
//import back.kalender.domain.schedule.entity.Schedule;
//import back.kalender.domain.schedule.entity.ScheduleCategory;
//import back.kalender.domain.schedule.repository.ScheduleRepository;
//import back.kalender.domain.user.entity.User;
//import back.kalender.domain.user.repository.UserRepository;
//import back.kalender.global.common.enums.Gender;
//import back.kalender.global.exception.ErrorCode;
//import back.kalender.global.exception.ServiceException;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("PartyService 테스트")
//class PartyServiceTest {
//
//    @InjectMocks
//    private PartyService partyService;
//
//    @Mock
//    private PartyRepository partyRepository;
//
//    @Mock
//    private PartyMemberRepository partyMemberRepository;
//
//    @Mock
//    private PartyApplicationRepository partyApplicationRepository;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private ScheduleRepository scheduleRepository;
//
//    private User testUser;
//    private User applicant;
//    private Schedule testSchedule;
//    private Party testParty;
//    private CreatePartyRequest createRequest;
//
//    @BeforeEach
//    void setUp() {
//        testUser = User.builder()
//                .email("test@example.com")
//                .password("password123")
//                .nickname("테스트유저")
//                .profileImage("https://example.com/profile.jpg")
//                .gender(Gender.MALE)
//                .level(1)
//                .birthDate(LocalDate.of(2000, 1, 1))
//                .emailVerified(true)
//                .build();
//
//        ReflectionTestUtils.setField(testUser, "id", 1L);
//
//        applicant = User.builder()
//                .email("applicant@example.com")
//                .password("password123")
//                .nickname("신청자")
//                .profileImage("https://example.com/applicant.jpg")
//                .gender(Gender.FEMALE)
//                .level(1)
//                .birthDate(LocalDate.of(2002, 5, 15))
//                .emailVerified(true)
//                .build();
//
//        ReflectionTestUtils.setField(applicant, "id", 2L);
//
//        testSchedule = Schedule.builder()
//                .artistId(1L)
//                .performanceId(10L)
//                .title("BTS 콘서트")
//                .scheduleCategory(ScheduleCategory.CONCERT)
//                .link("https://example.com/concert")
//                .scheduleTime(LocalDateTime.of(2025, 12, 31, 19, 0))
//                .location("잠실종합운동장")
//                .build();
//
//        ReflectionTestUtils.setField(testSchedule, "id", 100L);
//
//        testParty = Party.builder()
//                .scheduleId(100L)
//                .leaderId(1L)
//                .partyType(PartyType.LEAVE)
//                .partyName("즐거운 파티")
//                .description("같이 가요")
//                .departureLocation("강남역")
//                .arrivalLocation("잠실종합운동장")
//                .transportType(TransportType.SUBWAY)
//                .maxMembers(4)
//                .preferredGender(Gender.ANY)
//                .preferredAge(PreferredAge.TWENTY)
//                .build();
//
//        ReflectionTestUtils.setField(testParty, "id", 1L);
//
//        createRequest = new CreatePartyRequest(
//                100L,
//                PartyType.LEAVE,
//                "즐거운 파티",
//                "같이 가요",
//                "강남역",
//                "잠실종합운동장",
//                TransportType.SUBWAY,
//                4,
//                Gender.ANY,
//                PreferredAge.TWENTY
//        );
//    }
//
//    @Nested
//    @DisplayName("createParty 테스트")
//    class CreatePartyTest {
//
//        @Test
//        @DisplayName("성공: 파티 생성")
//        void createParty_Success() {
//            given(scheduleRepository.findById(100L)).willReturn(Optional.of(testSchedule));
//            given(partyRepository.save(any(Party.class))).willReturn(testParty);
//            given(partyMemberRepository.save(any(PartyMember.class))).willReturn(PartyMember.createLeader(1L, 1L));
//
//            CreatePartyResponse response = partyService.createParty(createRequest, 1L);
//
//            assertThat(response).isNotNull();
//            assertThat(response.leaderId()).isEqualTo(1L);
//            assertThat(response.status()).isEqualTo("생성 완료");
//
//            verify(scheduleRepository, times(1)).findById(100L);
//            verify(partyRepository, times(1)).save(any(Party.class));
//            verify(partyMemberRepository, times(1)).save(any(PartyMember.class));
//        }
//
//        @Test
//        @DisplayName("실패: 존재하지 않는 스케줄")
//        void createParty_ScheduleNotFound() {
//            given(scheduleRepository.findById(100L)).willReturn(Optional.empty());
//
//            assertThatThrownBy(() -> partyService.createParty(createRequest, 1L))
//                    .isInstanceOf(ServiceException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
//        }
//    }
//
//    @Nested
//    @DisplayName("updateParty 테스트")
//    class UpdatePartyTest {
//
//        @Test
//        @DisplayName("성공: 파티 수정")
//        void updateParty_Success() {
//            UpdatePartyRequest updateRequest = new UpdatePartyRequest(
//                    "수정된 파티",
//                    "수정된 설명",
//                    "신논현역",
//                    "잠실종합운동장",
//                    TransportType.TAXI,
//                    5,
//                    Gender.FEMALE,
//                    PreferredAge.THIRTY
//            );
//
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//
//            UpdatePartyResponse response = partyService.updateParty(1L, updateRequest, 1L);
//
//            assertThat(response).isNotNull();
//            assertThat(response.status()).isEqualTo("수정 완료");
//        }
//
//        @Test
//        @DisplayName("실패: 파티장이 아닌 사용자")
//        void updateParty_NotLeader() {
//            UpdatePartyRequest updateRequest = new UpdatePartyRequest(
//                    "수정된 파티", null, null, null, null, null, null, null
//            );
//
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//
//            assertThatThrownBy(() -> partyService.updateParty(1L, updateRequest, 999L))
//                    .isInstanceOf(ServiceException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_MODIFY_PARTY_NOT_LEADER);
//        }
//
//        @Test
//        @DisplayName("실패: 현재 인원보다 적게 최대 인원 설정")
//        void updateParty_CannotReduceMaxMembers() {
//            testParty.incrementCurrentMembers(); // 현재 인원 2명
//            testParty.incrementCurrentMembers(); // 현재 인원 3명
//
//            UpdatePartyRequest updateRequest = new UpdatePartyRequest(
//                    null, null, null, null, null, 2, null, null // 최대 인원 2명으로 변경 시도
//            );
//
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//
//            assertThatThrownBy(() -> partyService.updateParty(1L, updateRequest, 1L))
//                    .isInstanceOf(ServiceException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_REDUCE_MAX_MEMBERS);
//        }
//    }
//
//    @Nested
//    @DisplayName("deleteParty 테스트")
//    class DeletePartyTest {
//
//        @Test
//        @DisplayName("성공: 파티 삭제")
//        void deleteParty_Success() {
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//
//            partyService.deleteParty(1L, 1L);
//
//            verify(partyRepository, times(1)).delete(testParty);
//        }
//
//        @Test
//        @DisplayName("실패: 파티장이 아닌 사용자")
//        void deleteParty_NotLeader() {
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//
//            assertThatThrownBy(() -> partyService.deleteParty(1L, 999L))
//                    .isInstanceOf(ServiceException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_DELETE_PARTY_NOT_LEADER);
//        }
//    }
//
//    @Nested
//    @DisplayName("applyToParty 테스트")
//    class ApplyToPartyTest {
//
//        @Test
//        @DisplayName("성공: 파티 신청")
//        void applyToParty_Success() {
//            Long applicantId = 2L;
//
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//            given(partyApplicationRepository.existsByPartyIdAndApplicantId(1L, applicantId)).willReturn(false);
//            given(partyMemberRepository.existsActiveMember(1L, applicantId)).willReturn(false);
//            given(partyApplicationRepository.save(any(PartyApplication.class)))
//                    .willReturn(PartyApplication.create(1L, applicantId, 1L));
//            given(userRepository.findById(applicantId)).willReturn(Optional.of(applicant));
//
//            ApplyToPartyResponse response = partyService.applyToParty(1L, applicantId);
//
//            assertThat(response).isNotNull();
//            assertThat(response.applicantNickName()).isEqualTo("신청자");
//            assertThat(response.partyTitle()).isEqualTo("즐거운 파티");
//            assertThat(response.gender()).isEqualTo(Gender.FEMALE);
//        }
//
//        @Test
//        @DisplayName("실패: 본인 파티에 신청")
//        void applyToParty_OwnParty() {
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//
//            assertThatThrownBy(() -> partyService.applyToParty(1L, 1L))
//                    .isInstanceOf(ServiceException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_APPLY_OWN_PARTY);
//        }
//
//        @Test
//        @DisplayName("실패: 이미 신청한 파티")
//        void applyToParty_AlreadyApplied() {
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//            given(partyApplicationRepository.existsByPartyIdAndApplicantId(1L, 2L)).willReturn(true);
//
//            assertThatThrownBy(() -> partyService.applyToParty(1L, 2L))
//                    .isInstanceOf(ServiceException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_APPLIED);
//        }
//
//        @Test
//        @DisplayName("실패: 이미 파티 멤버")
//        void applyToParty_AlreadyMember() {
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//            given(partyApplicationRepository.existsByPartyIdAndApplicantId(1L, 2L)).willReturn(false);
//            given(partyMemberRepository.existsActiveMember(1L, 2L)).willReturn(true);
//
//            assertThatThrownBy(() -> partyService.applyToParty(1L, 2L))
//                    .isInstanceOf(ServiceException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_MEMBER);
//        }
//
//        @Test
//        @DisplayName("실패: 파티 인원 가득 참")
//        void applyToParty_PartyFull() {
//            testParty.incrementCurrentMembers();
//            testParty.incrementCurrentMembers();
//            testParty.incrementCurrentMembers();
//
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//            given(partyApplicationRepository.existsByPartyIdAndApplicantId(1L, 2L)).willReturn(false);
//            given(partyMemberRepository.existsActiveMember(1L, 2L)).willReturn(false);
//
//            assertThatThrownBy(() -> partyService.applyToParty(1L, 2L))
//                    .isInstanceOf(ServiceException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTY_FULL);
//        }
//
//        @Test
//        @DisplayName("실패: 모집중이 아닌 파티")
//        void applyToParty_NotRecruiting() {
//            testParty.changeStatus(PartyStatus.CLOSED);
//
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//            given(partyApplicationRepository.existsByPartyIdAndApplicantId(1L, 2L)).willReturn(false);
//            given(partyMemberRepository.existsActiveMember(1L, 2L)).willReturn(false);
//
//            assertThatThrownBy(() -> partyService.applyToParty(1L, 2L))
//                    .isInstanceOf(ServiceException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTY_NOT_RECRUITING);
//        }
//    }
//
//    @Nested
//    @DisplayName("acceptApplication 테스트")
//    class AcceptApplicationTest {
//
//        @Test
//        @DisplayName("성공: 신청 승인")
//        void acceptApplication_Success() {
//            Long applicationId = 10L;
//            Long applicantId = 2L;
//
//            PartyApplication application = PartyApplication.create(1L, applicantId, 1L);
//            ReflectionTestUtils.setField(application, "id", applicationId);
//
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//            given(partyApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));
//            given(partyMemberRepository.save(any(PartyMember.class)))
//                    .willReturn(PartyMember.createMember(1L, applicantId));
//
//            AcceptApplicationResponse response = partyService.acceptApplication(1L, applicationId, 1L);
//
//            assertThat(response).isNotNull();
//            assertThat(response.applicantId()).isEqualTo(applicantId);
//            assertThat(application.isApproved()).isTrue();
//            verify(partyMemberRepository, times(1)).save(any(PartyMember.class));
//        }
//
//        @Test
//        @DisplayName("성공: 승인 후 파티 인원이 가득 차면 모집 마감")
//        void acceptApplication_PartyFullAfterAccept() {
//            testParty.incrementCurrentMembers();
//            testParty.incrementCurrentMembers();
//
//            Long applicationId = 10L;
//            PartyApplication application = PartyApplication.create(1L, 2L, 1L);
//            ReflectionTestUtils.setField(application, "id", applicationId);
//
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//            given(partyApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));
//            given(partyMemberRepository.save(any(PartyMember.class)))
//                    .willReturn(PartyMember.createMember(1L, 2L));
//
//            partyService.acceptApplication(1L, applicationId, 1L);
//
//            assertThat(testParty.getStatus()).isEqualTo(PartyStatus.CLOSED);
//        }
//
//        @Test
//        @DisplayName("실패: 파티장이 아닌 사용자")
//        void acceptApplication_NotLeader() {
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//
//            assertThatThrownBy(() -> partyService.acceptApplication(1L, 10L, 999L))
//                    .isInstanceOf(ServiceException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_PARTY_LEADER);
//        }
//
//        @Test
//        @DisplayName("실패: 이미 처리된 신청")
//        void acceptApplication_AlreadyProcessed() {
//            PartyApplication application = PartyApplication.create(1L, 2L, 1L);
//            application.approve();
//
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//            given(partyApplicationRepository.findById(10L)).willReturn(Optional.of(application));
//
//            assertThatThrownBy(() -> partyService.acceptApplication(1L, 10L, 1L))
//                    .isInstanceOf(ServiceException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_ALREADY_PROCESSED);
//        }
//    }
//
//    @Nested
//    @DisplayName("getParties 테스트")
//    class GetPartiesTest {
//
//        @Test
//        @DisplayName("성공: 파티 목록 조회 (N+1 해결 확인)")
//        void getParties_Success() {
//            Pageable pageable = PageRequest.of(0, 20);
//            List<Party> parties = List.of(testParty);
//            Page<Party> partyPage = new PageImpl<>(parties, pageable, 1);
//
//            given(partyRepository.findAll(pageable)).willReturn(partyPage);
//            given(userRepository.findAllById(anyList())).willReturn(List.of(testUser));
//            given(scheduleRepository.findAllById(anyList())).willReturn(List.of(testSchedule));
//            given(partyApplicationRepository.findAppliedPartyIds(anyList(), anyLong())).willReturn(List.of());
//
//            GetPartiesResponse response = partyService.getParties(pageable, 1L);
//
//            assertThat(response).isNotNull();
//            assertThat(response.content()).hasSize(1);
//            assertThat(response.totalElements()).isEqualTo(1);
//
//            verify(userRepository, times(1)).findAllById(anyList());
//            verify(scheduleRepository, times(1)).findAllById(anyList());
//            verify(partyApplicationRepository, times(1)).findAppliedPartyIds(anyList(), anyLong());
//        }
//    }
//
//    @Nested
//    @DisplayName("getApplicants 테스트")
//    class GetApplicantsTest {
//
//        @Test
//        @DisplayName("성공: 신청자 목록 조회")
//        void getApplicants_Success() {
//            PartyApplication application = PartyApplication.create(1L, 2L, 1L);
//            List<PartyApplication> applications = List.of(application);
//
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//            given(partyApplicationRepository.findByPartyId(1L)).willReturn(applications);
//            given(userRepository.findAllById(anyList())).willReturn(List.of(applicant));
//            given(partyApplicationRepository.countPendingApplications(1L)).willReturn(1L);
//            given(partyApplicationRepository.countApprovedApplications(1L)).willReturn(0L);
//            given(partyApplicationRepository.countRejectedApplications(1L)).willReturn(0L);
//
//            GetApplicantsResponse response = partyService.getApplicants(1L, 1L);
//
//            assertThat(response).isNotNull();
//            assertThat(response.applications()).hasSize(1);
//            assertThat(response.summary().totalApplications()).isEqualTo(1);
//            assertThat(response.summary().pendingCount()).isEqualTo(1);
//
//            verify(userRepository, times(1)).findAllById(anyList());
//        }
//
//        @Test
//        @DisplayName("실패: 파티장이 아닌 사용자")
//        void getApplicants_NotLeader() {
//            given(partyRepository.findById(1L)).willReturn(Optional.of(testParty));
//
//            assertThatThrownBy(() -> partyService.getApplicants(1L, 999L))
//                    .isInstanceOf(ServiceException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_PARTY_LEADER);
//        }
//    }
//}