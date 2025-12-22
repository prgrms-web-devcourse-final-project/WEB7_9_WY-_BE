package back.kalender.domain.chat.service;

import back.kalender.domain.chat.dto.request.SendMessageRequest;
import back.kalender.domain.chat.dto.response.*;
import back.kalender.domain.chat.entity.ChatMessage;
import back.kalender.domain.chat.entity.ChatRoom;
import back.kalender.domain.chat.enums.MessageType;
import back.kalender.domain.chat.repository.ChatMessageRepository;
import back.kalender.domain.chat.repository.ChatRoomRepository;
import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.entity.PartyMember;
import back.kalender.domain.party.enums.PartyType;
import back.kalender.domain.party.enums.PreferredAge;
import back.kalender.domain.party.enums.TransportType;
import back.kalender.domain.party.repository.PartyMemberRepository;
import back.kalender.domain.party.repository.PartyRepository;
import back.kalender.domain.party.service.PartyService;
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

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 테스트")
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private PartyMemberRepository partyMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private PartyService partyService;

    private User testUser;
    private User targetUser;
    private Party testParty;
    private ChatRoom testChatRoom;
    private ChatMessage testMessage;

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

        targetUser = User.builder()
                .email("target@example.com")
                .password("password")
                .nickname("타겟유저")
                .birthDate(LocalDate.of(1995, 5, 5))
                .gender(Gender.FEMALE)
                .build();
        setId(targetUser, 2L);

        testParty = Party.builder()
                .scheduleId(1L)
                .leaderId(1L)
                .partyType(PartyType.LEAVE)
                .partyName("테스트 파티")
                .description("테스트")
                .departureLocation("강남역")
                .arrivalLocation("잠실")
                .transportType(TransportType.TAXI)
                .maxMembers(4)
                .preferredGender(Gender.ANY)
                .preferredAge(PreferredAge.ANY)
                .build();
        setId(testParty, 1L);

        testChatRoom = ChatRoom.create(1L, "테스트 파티");
        setId(testChatRoom, 1L);

        testMessage = ChatMessage.createChatMessage(1L, 1L, "안녕하세요");
        setId(testMessage, 1L);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Nested
    @DisplayName("채팅방 입장 테스트")
    class JoinRoomTest {

        @Test
        @DisplayName("성공: 파티 멤버가 채팅방에 입장한다")
        void joinRoom_Success() {
            Long partyId = 1L;
            String userEmail = "test@example.com";
            Long userId = 1L;

            given(userRepository.findByEmail(userEmail))
                    .willReturn(Optional.of(testUser));
            given(partyMemberRepository.existsActiveMember(partyId, userId))
                    .willReturn(true);
            given(partyRepository.findById(partyId))
                    .willReturn(Optional.of(testParty));
            given(chatRoomRepository.findActiveByPartyId(partyId))
                    .willReturn(Optional.of(testChatRoom));
            given(chatMessageRepository.save(any(ChatMessage.class)))
                    .willReturn(testMessage);

            RoomJoinedResponse response = chatService.joinRoom(partyId, userEmail);

            assertThat(response.type()).isEqualTo(MessageType.JOIN);
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(testParty.getLeaderId()).isEqualTo(1L); // Party 조회 의미 명확화

            then(partyMemberRepository)
                    .should().existsActiveMember(partyId, userId);

            then(chatMessageRepository).should().save(argThat(msg ->
                    msg.getMessageType() == MessageType.JOIN &&
                            msg.getSenderId().equals(userId)
            ));
        }


        @Test
        @DisplayName("실패: 존재하지 않는 유저")
        void joinRoom_UserNotFound() {
            Long partyId = 1L;
            String userEmail = "notfound@example.com";

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.joinRoom(partyId, userEmail))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패: 파티 멤버가 아님")
        void joinRoom_NotPartyMember() {
            Long partyId = 1L;
            String userEmail = "test@example.com";

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(partyMemberRepository.existsActiveMember(partyId, 1L)).willReturn(false);

            assertThatThrownBy(() -> chatService.joinRoom(partyId, userEmail))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.UNAUTHORIZED_PARTY_ACCESS.getMessage());
        }

        @Test
        @DisplayName("실패: 비활성화된 채팅방")
        void joinRoom_ChatRoomNotActive() {
            Long partyId = 1L;
            String userEmail = "test@example.com";

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(partyMemberRepository.existsActiveMember(partyId, 1L)).willReturn(true);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            given(chatRoomRepository.findActiveByPartyId(partyId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.joinRoom(partyId, userEmail))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.CHAT_ROOM_NOT_ACTIVE.getMessage());
        }
    }

    @Nested
    @DisplayName("메시지 전송 테스트")
    class SendMessageTest {

        @Test
        @DisplayName("성공: 채팅 메시지를 전송한다")
        void sendMessage_Success() {
            Long partyId = 1L;
            String userEmail = "test@example.com";
            SendMessageRequest request = new SendMessageRequest("안녕하세요!");

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(partyMemberRepository.existsActiveMember(partyId, 1L)).willReturn(true);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(testMessage);

            ChatMessageResponse response = chatService.sendMessage(partyId, request, userEmail);

            assertThat(response).isNotNull();
            assertThat(response.type()).isEqualTo(MessageType.CHAT);
            assertThat(response.senderId()).isEqualTo(1L);
            assertThat(response.senderNickname()).isEqualTo("테스터");
            assertThat(response.message()).isEqualTo("안녕하세요!");

            then(chatMessageRepository).should().save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("실패: 파티 멤버가 아닌 사용자가 메시지 전송")
        void sendMessage_NotPartyMember() {
            Long partyId = 1L;
            String userEmail = "test@example.com";
            SendMessageRequest request = new SendMessageRequest("메시지");

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(partyMemberRepository.existsActiveMember(partyId, 1L)).willReturn(false);

            assertThatThrownBy(() -> chatService.sendMessage(partyId, request, userEmail))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.UNAUTHORIZED_PARTY_ACCESS.getMessage());
        }
    }

    @Nested
    @DisplayName("채팅방 퇴장 테스트")
    class LeaveRoomTest {

        @Test
        @DisplayName("성공: 일반 멤버가 채팅방을 나간다")
        void leaveRoom_Success() {
            Long partyId = 1L;
            String userEmail = "target@example.com";
            Long userId = 2L;

            given(userRepository.findByEmail(userEmail))
                    .willReturn(Optional.of(targetUser));
            given(partyMemberRepository.existsActiveMember(partyId, userId))
                    .willReturn(true);
            given(partyRepository.findById(partyId))
                    .willReturn(Optional.of(testParty));
            given(chatMessageRepository.save(any(ChatMessage.class)))
                    .willReturn(testMessage);

            LeaveRoomResponse response = chatService.leaveRoom(partyId, userEmail);

            assertThat(response.type()).isEqualTo(MessageType.LEAVE);
            assertThat(testParty.getLeaderId()).isNotEqualTo(userId); // Party 조회 이유 명확

            then(partyMemberRepository)
                    .should().existsActiveMember(partyId, userId);

            then(chatMessageRepository).should().save(argThat(msg ->
                    msg.getMessageType() == MessageType.LEAVE &&
                            msg.getSenderId().equals(userId)
            ));
        }

        @Test
        @DisplayName("실패: 파티장은 채팅방을 나갈 수 없다")
        void leaveRoom_LeaderCannotLeave() {
            Long partyId = 1L;
            String userEmail = "test@example.com";  // 파티장

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(partyMemberRepository.existsActiveMember(partyId, 1L)).willReturn(true);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));

            assertThatThrownBy(() -> chatService.leaveRoom(partyId, userEmail))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.LEADER_CANNOT_LEAVE.getMessage());

            then(partyService).should(never()).removePartyMember(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("멤버 강퇴 테스트")
    class KickMemberTest {

        @Test
        @DisplayName("성공: 파티장이 멤버를 강퇴한다")
        void kickMember_Success() {
            Long partyId = 1L;
            Long targetMemberId = 2L;
            String leaderEmail = "test@example.com";

            given(userRepository.findByEmail(leaderEmail)).willReturn(Optional.of(testUser));
            given(partyMemberRepository.existsActiveMember(partyId, 1L)).willReturn(true);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            given(userRepository.findById(targetMemberId)).willReturn(Optional.of(targetUser));
            given(partyMemberRepository.existsActiveMember(partyId, targetMemberId))
                    .willReturn(true);
            willDoNothing().given(partyService).kickPartyMember(partyId, targetMemberId);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(testMessage);

            KickMemberResponse response = chatService.kickMember(
                    partyId, targetMemberId, leaderEmail);

            assertThat(response).isNotNull();
            assertThat(response.type()).isEqualTo(MessageType.KICK);
            assertThat(response.kickedMemberId()).isEqualTo(targetMemberId);
            assertThat(response.kickedMemberNickname()).isEqualTo("타겟유저");
            assertThat(response.kickedByLeaderId()).isEqualTo(1L);
            assertThat(response.kickedByLeaderNickname()).isEqualTo("테스터");

            then(partyService).should().kickPartyMember(partyId, targetMemberId);
            then(chatMessageRepository).should().save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("실패: 파티장이 아닌 사용자가 강퇴 시도")
        void kickMember_OnlyLeaderCanKick() {
            Long partyId = 1L;
            Long targetMemberId = 3L;
            String memberEmail = "target@example.com";  // 일반 멤버

            given(userRepository.findByEmail(memberEmail)).willReturn(Optional.of(targetUser));
            given(partyMemberRepository.existsActiveMember(partyId, 2L)).willReturn(true);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));

            assertThatThrownBy(() -> chatService.kickMember(
                    partyId, targetMemberId, memberEmail))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.ONLY_LEADER_CAN_KICK.getMessage());
        }

        @Test
        @DisplayName("실패: 자기 자신을 강퇴할 수 없다")
        void kickMember_CannotKickYourself() {
            Long partyId = 1L;
            Long targetMemberId = 1L;  // 파티장 자신
            String leaderEmail = "test@example.com";

            given(userRepository.findByEmail(leaderEmail)).willReturn(Optional.of(testUser));
            given(partyMemberRepository.existsActiveMember(partyId, 1L)).willReturn(true);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));

            assertThatThrownBy(() -> chatService.kickMember(
                    partyId, targetMemberId, leaderEmail))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.CANNOT_KICK_YOURSELF.getMessage());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 멤버를 강퇴 시도")
        void kickMember_UserNotInParty() {
            Long partyId = 1L;
            Long targetMemberId = 999L;
            String leaderEmail = "test@example.com";

            given(userRepository.findByEmail(leaderEmail)).willReturn(Optional.of(testUser));
            given(partyMemberRepository.existsActiveMember(partyId, 1L)).willReturn(true);
            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            given(userRepository.findById(targetMemberId)).willReturn(Optional.of(targetUser));
            given(partyMemberRepository.existsActiveMember(partyId, targetMemberId))
                    .willReturn(false);

            assertThatThrownBy(() -> chatService.kickMember(
                    partyId, targetMemberId, leaderEmail))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.USER_NOT_IN_PARTY.getMessage());
        }
    }
}