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
import back.kalender.global.common.Enum.Gender;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartyService 테스트")
class PartyServiceImplTest {

    @InjectMocks
    private PartyServiceImpl partyService;

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

    @Nested
    @DisplayName("createParty 테스트")
    class CreatePartyTest {

        @Test
        @DisplayName("파티 생성 성공")
        void createParty_Success() {
            Long currentUserId = 1L;
            Long scheduleId = 100L;

            CreatePartyRequest request = new CreatePartyRequest(
                    scheduleId,
                    PartyType.LEAVE,
                    "지민이 최애",
                    "같이 즐겁게 콘서트 가요!",
                    "강남역 3번출구",
                    "잠실종합운동장",
                    TransportType.TAXI,
                    4,
                    Gender.FEMALE,
                    PreferredAge.TWENTY
            );

            Schedule schedule = mock(Schedule.class);
            Party party = mock(Party.class);
            PartyMember leader = mock(PartyMember.class);

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(partyRepository.save(any(Party.class))).willReturn(party);
            given(party.getId()).willReturn(1L);
            given(party.getLeaderId()).willReturn(currentUserId);
            given(partyMemberRepository.save(any(PartyMember.class))).willReturn(leader);

            CreatePartyResponse response = partyService.createParty(request, currentUserId);

            assertThat(response).isNotNull();
            assertThat(response.partyId()).isEqualTo(1L);
            assertThat(response.leaderId()).isEqualTo(currentUserId);
            assertThat(response.status()).isEqualTo("생성 완료");

            then(scheduleRepository).should(times(1)).findById(scheduleId);
            then(partyRepository).should(times(1)).save(any(Party.class));
            then(partyMemberRepository).should(times(1)).save(any(PartyMember.class));
        }

        @Test
        @DisplayName("파티 생성 실패 - 일정을 찾을 수 없음")
        void createParty_Fail_ScheduleNotFound() {
            Long currentUserId = 1L;
            Long scheduleId = 999L;

            CreatePartyRequest request = new CreatePartyRequest(
                    scheduleId,
                    PartyType.LEAVE,
                    "지민이 최애",
                    "같이 즐겁게 콘서트 가요!",
                    "강남역 3번출구",
                    "잠실종합운동장",
                    TransportType.TAXI,
                    4,
                    Gender.FEMALE,
                    PreferredAge.TWENTY
            );

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.createParty(request, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);

            then(scheduleRepository).should(times(1)).findById(scheduleId);
            then(partyRepository).should(never()).save(any(Party.class));
        }
    }

    @Nested
    @DisplayName("updateParty 테스트")
    class UpdatePartyTest {

        @Test
        @DisplayName("파티 수정 성공")
        void updateParty_Success() {
            Long partyId = 1L;
            Long currentUserId = 1L;

            UpdatePartyRequest request = new UpdatePartyRequest(
                    "새로운 파티 이름",
                    "새로운 설명",
                    "신논현역",
                    "올림픽공원",
                    TransportType.SUBWAY,
                    5,
                    Gender.ANY,
                    PreferredAge.THIRTY
            );

            Party party = mock(Party.class);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(true);
            given(party.getCurrentMembers()).willReturn(3);
            given(party.getId()).willReturn(partyId);
            given(party.getLeaderId()).willReturn(currentUserId);

            UpdatePartyResponse response = partyService.updateParty(partyId, request, currentUserId);

            assertThat(response).isNotNull();
            assertThat(response.partyId()).isEqualTo(partyId);
            assertThat(response.leaderId()).isEqualTo(currentUserId);
            assertThat(response.status()).isEqualTo("수정 완료");

            then(partyRepository).should(times(1)).findById(partyId);
            then(party).should(times(1)).update(any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("파티 수정 실패 - 파티를 찾을 수 없음")
        void updateParty_Fail_PartyNotFound() {
            Long partyId = 999L;
            Long currentUserId = 1L;

            UpdatePartyRequest request = new UpdatePartyRequest(
                    "새로운 파티 이름",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            given(partyRepository.findById(partyId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.updateParty(partyId, request, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTY_NOT_FOUND);

            then(partyRepository).should(times(1)).findById(partyId);
        }

        @Test
        @DisplayName("파티 수정 실패 - 파티장이 아님")
        void updateParty_Fail_NotLeader() {
            Long partyId = 1L;
            Long currentUserId = 2L;

            UpdatePartyRequest request = new UpdatePartyRequest(
                    "새로운 파티 이름",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            Party party = mock(Party.class);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(false);

            assertThatThrownBy(() -> partyService.updateParty(partyId, request, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_MODIFY_PARTY_NOT_LEADER);

            then(partyRepository).should(times(1)).findById(partyId);
        }

        @Test
        @DisplayName("파티 수정 실패 - 현재 인원보다 적게 최대 인원 설정")
        void updateParty_Fail_CannotReduceMaxMembers() {
            Long partyId = 1L;
            Long currentUserId = 1L;

            UpdatePartyRequest request = new UpdatePartyRequest(
                    null,
                    null,
                    null,
                    null,
                    null,
                    2, // 현재 인원(3)보다 적음
                    null,
                    null
            );

            Party party = mock(Party.class);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(true);
            given(party.getCurrentMembers()).willReturn(3);

            assertThatThrownBy(() -> partyService.updateParty(partyId, request, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_REDUCE_MAX_MEMBERS);

            then(partyRepository).should(times(1)).findById(partyId);
        }
    }

    @Nested
    @DisplayName("deleteParty 테스트")
    class DeletePartyTest {

        @Test
        @DisplayName("파티 삭제 성공")
        void deleteParty_Success() {
            Long partyId = 1L;
            Long currentUserId = 1L;

            Party party = mock(Party.class);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(true);

            partyService.deleteParty(partyId, currentUserId);

            then(partyRepository).should(times(1)).findById(partyId);
            then(partyRepository).should(times(1)).delete(party);
        }

        @Test
        @DisplayName("파티 삭제 실패 - 파티를 찾을 수 없음")
        void deleteParty_Fail_PartyNotFound() {
            Long partyId = 999L;
            Long currentUserId = 1L;

            given(partyRepository.findById(partyId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.deleteParty(partyId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTY_NOT_FOUND);

            then(partyRepository).should(times(1)).findById(partyId);
            then(partyRepository).should(never()).delete(any(Party.class));
        }

        @Test
        @DisplayName("파티 삭제 실패 - 파티장이 아님")
        void deleteParty_Fail_NotLeader() {
            Long partyId = 1L;
            Long currentUserId = 2L;

            Party party = mock(Party.class);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(false);

            assertThatThrownBy(() -> partyService.deleteParty(partyId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_DELETE_PARTY_NOT_LEADER);

            then(partyRepository).should(times(1)).findById(partyId);
            then(partyRepository).should(never()).delete(any(Party.class));
        }
    }

    @Nested
    @DisplayName("getParties 테스트")
    class GetPartiesTest {

        @Test
        @DisplayName("파티 목록 조회 성공")
        void getParties_Success() {
            Long currentUserId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            Party party = mock(Party.class);
            User leader = mock(User.class);
            Schedule schedule = mock(Schedule.class);

            given(party.getId()).willReturn(1L);
            given(party.getLeaderId()).willReturn(2L);
            given(party.getScheduleId()).willReturn(100L);
            given(party.getPartyType()).willReturn(PartyType.LEAVE);
            given(party.getPartyName()).willReturn("지민이 최애");
            given(party.getDepartureLocation()).willReturn("강남역");
            given(party.getArrivalLocation()).willReturn("잠실");
            given(party.getTransportType()).willReturn(TransportType.TAXI);
            given(party.getMaxMembers()).willReturn(4);
            given(party.getCurrentMembers()).willReturn(2);
            given(party.getDescription()).willReturn("설명");
            given(party.getStatus()).willReturn(PartyStatus.RECRUITING);

            given(leader.getNickname()).willReturn("리더");
            given(leader.getAge()).willReturn(25);
            given(leader.getGender()).willReturn(Gender.FEMALE);
            given(leader.getProfileImage()).willReturn("image.jpg");

            given(schedule.getTitle()).willReturn("BTS 콘서트");
            given(schedule.getLocation()).willReturn("잠실종합운동장");

            Page<Party> partyPage = new PageImpl<>(List.of(party), pageable, 1);

            given(partyRepository.findAll(pageable)).willReturn(partyPage);
            given(userRepository.findById(2L)).willReturn(Optional.of(leader));
            given(scheduleRepository.findById(100L)).willReturn(Optional.of(schedule));
            given(partyApplicationRepository.existsByPartyIdAndApplicantId(1L, currentUserId)).willReturn(false);

            GetPartiesResponse response = partyService.getParties(pageable, currentUserId);

            assertThat(response).isNotNull();
            assertThat(response.content()).hasSize(1);
            assertThat(response.totalElements()).isEqualTo(1);
            assertThat(response.totalPages()).isEqualTo(1);
            assertThat(response.pageNumber()).isEqualTo(0);

            GetPartiesResponse.PartyItem partyItem = response.content().get(0);
            assertThat(partyItem.partyId()).isEqualTo(1L);
            assertThat(partyItem.isMyParty()).isFalse();
            assertThat(partyItem.isApplied()).isFalse();

            then(partyRepository).should(times(1)).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("applyToParty 테스트")
    class ApplyToPartyTest {

        @Test
        @DisplayName("파티 신청 성공")
        void applyToParty_Success() {
            Long partyId = 1L;
            Long currentUserId = 2L;

            Party party = mock(Party.class);
            User user = mock(User.class);
            PartyApplication application = mock(PartyApplication.class);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(false);
            given(party.getLeaderId()).willReturn(1L);
            given(party.getPartyName()).willReturn("지민이 최애");
            given(party.isFull()).willReturn(false);
            given(party.isRecruiting()).willReturn(true);
            given(partyApplicationRepository.existsByPartyIdAndApplicantId(partyId, currentUserId)).willReturn(false);
            given(partyMemberRepository.existsActiveMember(partyId, currentUserId)).willReturn(false);
            given(partyApplicationRepository.save(any(PartyApplication.class))).willReturn(application);

            given(userRepository.findById(currentUserId)).willReturn(Optional.of(user));
            given(user.getNickname()).willReturn("신청자");
            given(user.getAge()).willReturn(23);
            given(user.getGender()).willReturn(Gender.FEMALE);

            ApplyToPartyResponse response = partyService.applyToParty(partyId, currentUserId);

            assertThat(response).isNotNull();
            assertThat(response.applicantNickName()).isEqualTo("신청자");
            assertThat(response.applicantAge()).isEqualTo(23);
            assertThat(response.gender()).isEqualTo(Gender.FEMALE);
            assertThat(response.partyTitle()).isEqualTo("지민이 최애");

            then(partyRepository).should(times(1)).findById(partyId);
            then(partyApplicationRepository).should(times(1)).save(any(PartyApplication.class));
        }

        @Test
        @DisplayName("파티 신청 실패 - 파티를 찾을 수 없음")
        void applyToParty_Fail_PartyNotFound() {
            Long partyId = 999L;
            Long currentUserId = 2L;

            given(partyRepository.findById(partyId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.applyToParty(partyId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTY_NOT_FOUND);
        }

        @Test
        @DisplayName("파티 신청 실패 - 본인이 만든 파티")
        void applyToParty_Fail_OwnParty() {
            Long partyId = 1L;
            Long currentUserId = 1L;

            Party party = mock(Party.class);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(true);

            assertThatThrownBy(() -> partyService.applyToParty(partyId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_APPLY_OWN_PARTY);
        }

        @Test
        @DisplayName("파티 신청 실패 - 이미 신청함")
        void applyToParty_Fail_AlreadyApplied() {
            Long partyId = 1L;
            Long currentUserId = 2L;

            Party party = mock(Party.class);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(false);
            given(partyApplicationRepository.existsByPartyIdAndApplicantId(partyId, currentUserId)).willReturn(true);

            assertThatThrownBy(() -> partyService.applyToParty(partyId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_APPLIED);
        }

        @Test
        @DisplayName("파티 신청 실패 - 이미 멤버임")
        void applyToParty_Fail_AlreadyMember() {
            Long partyId = 1L;
            Long currentUserId = 2L;

            Party party = mock(Party.class);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(false);
            given(partyApplicationRepository.existsByPartyIdAndApplicantId(partyId, currentUserId)).willReturn(false);
            given(partyMemberRepository.existsActiveMember(partyId, currentUserId)).willReturn(true);

            assertThatThrownBy(() -> partyService.applyToParty(partyId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_MEMBER);
        }

        @Test
        @DisplayName("파티 신청 실패 - 파티 인원 가득 참")
        void applyToParty_Fail_PartyFull() {
            Long partyId = 1L;
            Long currentUserId = 2L;

            Party party = mock(Party.class);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(false);
            given(partyApplicationRepository.existsByPartyIdAndApplicantId(partyId, currentUserId)).willReturn(false);
            given(partyMemberRepository.existsActiveMember(partyId, currentUserId)).willReturn(false);
            given(party.isFull()).willReturn(true);

            assertThatThrownBy(() -> partyService.applyToParty(partyId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTY_FULL);
        }

        @Test
        @DisplayName("파티 신청 실패 - 모집중이 아님")
        void applyToParty_Fail_NotRecruiting() {
            Long partyId = 1L;
            Long currentUserId = 2L;

            Party party = mock(Party.class);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(false);
            given(partyApplicationRepository.existsByPartyIdAndApplicantId(partyId, currentUserId)).willReturn(false);
            given(partyMemberRepository.existsActiveMember(partyId, currentUserId)).willReturn(false);
            given(party.isFull()).willReturn(false);
            given(party.isRecruiting()).willReturn(false);

            assertThatThrownBy(() -> partyService.applyToParty(partyId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTY_NOT_RECRUITING);
        }
    }

    @Nested
    @DisplayName("cancelApplication 테스트")
    class CancelApplicationTest {

        @Test
        @DisplayName("신청 취소 성공")
        void cancelApplication_Success() {
            Long partyId = 1L;
            Long applicationId = 10L;
            Long currentUserId = 2L;

            PartyApplication application = mock(PartyApplication.class);
            given(partyApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));
            given(application.getApplicantId()).willReturn(currentUserId);
            given(application.isApproved()).willReturn(false);

            partyService.cancelApplication(partyId, applicationId, currentUserId);

            then(partyApplicationRepository).should(times(1)).findById(applicationId);
            then(partyApplicationRepository).should(times(1)).delete(application);
        }

        @Test
        @DisplayName("신청 취소 실패 - 신청을 찾을 수 없음")
        void cancelApplication_Fail_ApplicationNotFound() {
            Long partyId = 1L;
            Long applicationId = 999L;
            Long currentUserId = 2L;

            given(partyApplicationRepository.findById(applicationId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.cancelApplication(partyId, applicationId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("신청 취소 실패 - 권한 없음 (다른 사람의 신청)")
        void cancelApplication_Fail_Unauthorized() {
            Long partyId = 1L;
            Long applicationId = 10L;
            Long currentUserId = 2L;

            PartyApplication application = mock(PartyApplication.class);
            given(partyApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));
            given(application.getApplicantId()).willReturn(3L);

            assertThatThrownBy(() -> partyService.cancelApplication(partyId, applicationId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_PARTY_ACCESS);
        }

        @Test
        @DisplayName("신청 취소 실패 - 이미 승인된 신청")
        void cancelApplication_Fail_AlreadyApproved() {
            Long partyId = 1L;
            Long applicationId = 10L;
            Long currentUserId = 2L;

            PartyApplication application = mock(PartyApplication.class);
            given(partyApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));
            given(application.getApplicantId()).willReturn(currentUserId);
            given(application.isApproved()).willReturn(true);

            assertThatThrownBy(() -> partyService.cancelApplication(partyId, applicationId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_CANCEL_APPROVED_APPLICATION);
        }
    }

    @Nested
    @DisplayName("acceptApplication 테스트")
    class AcceptApplicationTest {

        @Test
        @DisplayName("신청 승인 성공")
        void acceptApplication_Success() {
            Long partyId = 1L;
            Long applicationId = 10L;
            Long currentUserId = 1L;
            Long applicantId = 2L;

            Party party = mock(Party.class);
            PartyApplication application = mock(PartyApplication.class);
            PartyMember member = mock(PartyMember.class);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(true);
            given(party.isFull()).willReturn(false);
            given(party.getPartyName()).willReturn("지민이 최애");

            given(partyApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));
            given(application.isProcessed()).willReturn(false);
            given(application.getApplicantId()).willReturn(applicantId);

            given(partyMemberRepository.save(any(PartyMember.class))).willReturn(member);

            AcceptApplicationResponse response = partyService.acceptApplication(partyId, applicationId, currentUserId);

            assertThat(response).isNotNull();
            assertThat(response.applicantId()).isEqualTo(applicantId);
            assertThat(response.partyTitle()).isEqualTo("지민이 최애");
            assertThat(response.chatRoomId()).isNull();

            then(application).should(times(1)).approve();
            then(partyMemberRepository).should(times(1)).save(any(PartyMember.class));
            then(party).should(times(1)).incrementCurrentMembers();
        }

        @Test
        @DisplayName("신청 승인 성공 - 인원 가득 차서 상태 변경")
        void acceptApplication_Success_PartyFull() {
            Long partyId = 1L;
            Long applicationId = 10L;
            Long currentUserId = 1L;
            Long applicantId = 2L;

            Party party = mock(Party.class);
            PartyApplication application = mock(PartyApplication.class);
            PartyMember member = mock(PartyMember.class);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(true);
            given(party.isFull()).willReturn(false).willReturn(true); // 승인 후 가득 참
            given(party.getPartyName()).willReturn("지민이 최애");

            given(partyApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));
            given(application.isProcessed()).willReturn(false);
            given(application.getApplicantId()).willReturn(applicantId);

            given(partyMemberRepository.save(any(PartyMember.class))).willReturn(member);

            AcceptApplicationResponse response = partyService.acceptApplication(partyId, applicationId, currentUserId);

            assertThat(response).isNotNull();
            then(party).should(times(1)).changeStatus(PartyStatus.CLOSED);
        }

        @Test
        @DisplayName("신청 승인 실패 - 파티를 찾을 수 없음")
        void acceptApplication_Fail_PartyNotFound() {
            Long partyId = 999L;
            Long applicationId = 10L;
            Long currentUserId = 1L;

            given(partyRepository.findById(partyId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.acceptApplication(partyId, applicationId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTY_NOT_FOUND);
        }

        @Test
        @DisplayName("신청 승인 실패 - 파티장이 아님")
        void acceptApplication_Fail_NotLeader() {
            Long partyId = 1L;
            Long applicationId = 10L;
            Long currentUserId = 2L;

            Party party = mock(Party.class);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(false);

            assertThatThrownBy(() -> partyService.acceptApplication(partyId, applicationId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_PARTY_LEADER);
        }

        @Test
        @DisplayName("신청 승인 실패 - 신청을 찾을 수 없음")
        void acceptApplication_Fail_ApplicationNotFound() {
            Long partyId = 1L;
            Long applicationId = 999L;
            Long currentUserId = 1L;

            Party party = mock(Party.class);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(true);
            given(partyApplicationRepository.findById(applicationId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.acceptApplication(partyId, applicationId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("신청 승인 실패 - 이미 처리된 신청")
        void acceptApplication_Fail_AlreadyProcessed() {
            Long partyId = 1L;
            Long applicationId = 10L;
            Long currentUserId = 1L;

            Party party = mock(Party.class);
            PartyApplication application = mock(PartyApplication.class);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(true);
            given(partyApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));
            given(application.isProcessed()).willReturn(true);

            assertThatThrownBy(() -> partyService.acceptApplication(partyId, applicationId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }

        @Test
        @DisplayName("신청 승인 실패 - 파티 인원 가득 참")
        void acceptApplication_Fail_PartyFull() {
            Long partyId = 1L;
            Long applicationId = 10L;
            Long currentUserId = 1L;

            Party party = mock(Party.class);
            PartyApplication application = mock(PartyApplication.class);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(true);
            given(partyApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));
            given(application.isProcessed()).willReturn(false);
            given(party.isFull()).willReturn(true);

            assertThatThrownBy(() -> partyService.acceptApplication(partyId, applicationId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTY_FULL);
        }
    }

    @Nested
    @DisplayName("rejectApplication 테스트")
    class RejectApplicationTest {

        @Test
        @DisplayName("신청 거절 성공")
        void rejectApplication_Success() {
            Long partyId = 1L;
            Long applicationId = 10L;
            Long currentUserId = 1L;
            Long applicantId = 2L;

            Party party = mock(Party.class);
            PartyApplication application = mock(PartyApplication.class);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(true);
            given(party.getPartyName()).willReturn("지민이 최애");

            given(partyApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));
            given(application.isProcessed()).willReturn(false);
            given(application.getApplicantId()).willReturn(applicantId);

            RejectApplicationResponse response = partyService.rejectApplication(partyId, applicationId, currentUserId);

            assertThat(response).isNotNull();
            assertThat(response.applicantId()).isEqualTo(applicantId);
            assertThat(response.partyName()).isEqualTo("지민이 최애");
            assertThat(response.chatRoomId()).isNull();

            then(application).should(times(1)).reject();
        }

        @Test
        @DisplayName("신청 거절 실패 - 파티를 찾을 수 없음")
        void rejectApplication_Fail_PartyNotFound() {
            Long partyId = 999L;
            Long applicationId = 10L;
            Long currentUserId = 1L;

            given(partyRepository.findById(partyId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.rejectApplication(partyId, applicationId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTY_NOT_FOUND);
        }

        @Test
        @DisplayName("신청 거절 실패 - 파티장이 아님")
        void rejectApplication_Fail_NotLeader() {
            Long partyId = 1L;
            Long applicationId = 10L;
            Long currentUserId = 2L;

            Party party = mock(Party.class);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(false);

            assertThatThrownBy(() -> partyService.rejectApplication(partyId, applicationId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_PARTY_LEADER);
        }

        @Test
        @DisplayName("신청 거절 실패 - 이미 처리된 신청")
        void rejectApplication_Fail_AlreadyProcessed() {
            Long partyId = 1L;
            Long applicationId = 10L;
            Long currentUserId = 1L;

            Party party = mock(Party.class);
            PartyApplication application = mock(PartyApplication.class);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(true);
            given(partyApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));
            given(application.isProcessed()).willReturn(true);

            assertThatThrownBy(() -> partyService.rejectApplication(partyId, applicationId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }
    }

    @Nested
    @DisplayName("getApplicants 테스트")
    class GetApplicantsTest {

        @Test
        @DisplayName("신청자 목록 조회 성공")
        void getApplicants_Success() {
            Long partyId = 1L;
            Long currentUserId = 1L;

            Party party = mock(Party.class);
            PartyApplication application = mock(PartyApplication.class);
            User user = mock(User.class);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(true);

            given(application.getId()).willReturn(10L);
            given(application.getApplicantId()).willReturn(2L);
            given(application.getStatus()).willReturn(ApplicationStatus.PENDING);

            given(user.getNickname()).willReturn("신청자");
            given(user.getProfileImage()).willReturn("image.jpg");
            given(user.getGender()).willReturn(Gender.FEMALE);
            given(user.getAge()).willReturn(23);

            given(partyApplicationRepository.findByPartyId(partyId)).willReturn(List.of(application));
            given(userRepository.findById(2L)).willReturn(Optional.of(user));
            given(partyApplicationRepository.countPendingApplications(partyId)).willReturn(1L);
            given(partyApplicationRepository.countApprovedApplications(partyId)).willReturn(0L);
            given(partyApplicationRepository.countRejectedApplications(partyId)).willReturn(0L);

            GetApplicantsResponse response = partyService.getApplicants(partyId, currentUserId);

            assertThat(response).isNotNull();
            assertThat(response.partyId()).isEqualTo(partyId);
            assertThat(response.applications()).hasSize(1);
            assertThat(response.summary().totalApplications()).isEqualTo(1);
            assertThat(response.summary().pendingCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("신청자 목록 조회 실패 - 파티를 찾을 수 없음")
        void getApplicants_Fail_PartyNotFound() {
            Long partyId = 999L;
            Long currentUserId = 1L;

            given(partyRepository.findById(partyId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.getApplicants(partyId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTY_NOT_FOUND);
        }

        @Test
        @DisplayName("신청자 목록 조회 실패 - 파티장이 아님")
        void getApplicants_Fail_NotLeader() {
            Long partyId = 1L;
            Long currentUserId = 2L;

            Party party = mock(Party.class);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));
            given(party.isLeader(currentUserId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> partyService.getApplicants(partyId, currentUserId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_PARTY_LEADER);
        }
    }

    @Nested
    @DisplayName("getPartyMembers 테스트")
    class GetPartyMembersTest {

        @Test
        @DisplayName("파티 멤버 목록 조회 성공")
        void getPartyMembers_Success() {
            Long partyId = 1L;

            Party party = mock(Party.class);
            PartyMember member = mock(PartyMember.class);
            User user = mock(User.class);

            given(partyRepository.findById(partyId)).willReturn(Optional.of(party));

            given(member.getId()).willReturn(1L);
            given(member.getUserId()).willReturn(100L);
            given(member.getRole()).willReturn(MemberRole.LEADER);

            given(user.getNickname()).willReturn("파티장");
            given(user.getProfileImage()).willReturn("image.jpg");

            given(partyMemberRepository.findActiveMembers(partyId)).willReturn(List.of(member));
            given(userRepository.findById(100L)).willReturn(Optional.of(user));

            GetPartyMembersResponse response = partyService.getPartyMembers(partyId);

            assertThat(response).isNotNull();
            assertThat(response.partyId()).isEqualTo(partyId);
            assertThat(response.members()).hasSize(1);
            assertThat(response.totalMembers()).isEqualTo(1);
        }

        @Test
        @DisplayName("파티 멤버 목록 조회 실패 - 파티를 찾을 수 없음")
        void getPartyMembers_Fail_PartyNotFound() {
            Long partyId = 999L;

            given(partyRepository.findById(partyId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> partyService.getPartyMembers(partyId))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTY_NOT_FOUND);
        }
    }
}