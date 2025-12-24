package back.kalender.domain.chat.service;

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
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import back.kalender.global.common.enums.Gender;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomService 테스트")
class ChatRoomServiceTest {

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private PartyMemberRepository partyMemberRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;
    private User otherUser;
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
                .profileImage("test.jpg")
                .build();
        setId(testUser, 1L);

        otherUser = User.builder()
                .email("other@example.com")
                .password("password")
                .nickname("다른유저")
                .birthDate(LocalDate.of(1995, 5, 5))
                .gender(Gender.FEMALE)
                .profileImage("other.jpg")
                .build();
        setId(otherUser, 2L);

        testParty = Party.builder()
                .scheduleId(1L)
                .leaderId(testUser.getId())
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
        setCreatedAt(testParty, LocalDateTime.now());
        testParty.incrementCurrentMembers(); // 2명

        testChatRoom = ChatRoom.create(1L, "테스트 파티");
        setId(testChatRoom, 1L);
        setCreatedAt(testChatRoom, LocalDateTime.now());

        testMessage = ChatMessage.createChatMessage(1L, 1L, "안녕하세요");
        setId(testMessage, 1L);
        setCreatedAt(testMessage, LocalDateTime.now());
    }

    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    private void setCreatedAt(Object entity, LocalDateTime createdAt) throws Exception {
        Field createdAtField = entity.getClass().getSuperclass().getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(entity, createdAt);
    }

    @Nested
    @DisplayName("채팅방 생성 테스트")
    class CreateChatRoomTest {

        @Test
        @DisplayName("성공: 채팅방을 생성하고 저장한다")
        void createChatRoom_Success() {
            
            Long partyId = 1L;
            String partyName = "테스트 파티";

            given(chatRoomRepository.existsByPartyId(partyId)).willReturn(false);
            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(testChatRoom);

            chatRoomService.createChatRoom(partyId, partyName);

            then(chatRoomRepository).should().existsByPartyId(partyId);
            then(chatRoomRepository).should().save(argThat(room ->
                    room.getPartyId().equals(partyId) &&
                            room.getRoomName().equals(partyName)
            ));
        }

        @Test
        @DisplayName("성공: 이미 존재하는 채팅방이면 생성하지 않는다")
        void createChatRoom_AlreadyExists() {
            
            Long partyId = 1L;
            String partyName = "테스트 파티";

            given(chatRoomRepository.existsByPartyId(partyId)).willReturn(true);
            
            chatRoomService.createChatRoom(partyId, partyName);

            then(chatRoomRepository).should().existsByPartyId(partyId);
            then(chatRoomRepository).should(never()).save(any(ChatRoom.class));
        }
    }

    @Nested
    @DisplayName("채팅방 종료 테스트")
    class CloseChatRoomTest {

        @Test
        @DisplayName("성공: 채팅방을 비활성화한다")
        void closeChatRoom_Success() {
            
            Long partyId = 1L;

            given(chatRoomRepository.findByPartyId(partyId))
                    .willReturn(Optional.of(testChatRoom));

            chatRoomService.closeChatRoom(partyId);
            
            assertThat(testChatRoom.getIsActive()).isFalse();
            then(chatRoomRepository).should().findByPartyId(partyId);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 채팅방")
        void closeChatRoom_ChatRoomNotFound() {
            
            Long partyId = 999L;

            given(chatRoomRepository.findByPartyId(partyId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> chatRoomService.closeChatRoom(partyId))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.CHAT_ROOM_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("채팅방 정보 조회 테스트")
    class GetChatRoomInfoTest {

        @Test
        @DisplayName("성공: 채팅방 정보를 조회한다")
        void getChatRoomInfo_Success() {
            
            Long partyId = 1L;
            String userEmail = "test@example.com";

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            given(partyMemberRepository.existsActiveMember(partyId, 1L)).willReturn(true);
            given(chatRoomRepository.findByPartyId(partyId))
                    .willReturn(Optional.of(testChatRoom));

            
            ChatRoomInfoResponse response = chatRoomService.getChatRoomInfo(partyId, userEmail);
            
            assertThat(response).isNotNull();
            assertThat(response.partyId()).isEqualTo(1L);
            assertThat(response.partyName()).isEqualTo("테스트 파티");
            assertThat(response.participantCount()).isEqualTo(2);
            assertThat(response.maxParticipants()).isEqualTo(4);
            assertThat(response.isActive()).isTrue();
            assertThat(response.createdAt()).isNotNull();

            then(userRepository).should().findByEmail(userEmail);
            then(partyRepository).should().findById(partyId);
            then(partyMemberRepository).should().existsActiveMember(partyId, 1L);
            then(chatRoomRepository).should().findByPartyId(partyId);
        }

        @Test
        @DisplayName("실패: 파티 멤버가 아닌 사용자")
        void getChatRoomInfo_NotPartyMember() {
            
            Long partyId = 1L;
            String userEmail = "test@example.com";

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            given(partyMemberRepository.existsActiveMember(partyId, 1L)).willReturn(false);

            assertThatThrownBy(() -> chatRoomService.getChatRoomInfo(partyId, userEmail))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.UNAUTHORIZED_PARTY_ACCESS.getMessage());
        }

        @Test
        @DisplayName("실패: 채팅방이 존재하지 않음")
        void getChatRoomInfo_ChatRoomNotFound() {
            
            Long partyId = 1L;
            String userEmail = "test@example.com";

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            given(partyMemberRepository.existsActiveMember(partyId, 1L)).willReturn(true);
            given(chatRoomRepository.findByPartyId(partyId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> chatRoomService.getChatRoomInfo(partyId, userEmail))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining(ErrorCode.CHAT_ROOM_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("참여자 목록 조회 테스트")
    class GetParticipantsTest {

        @Test
        @DisplayName("성공: 참여자 목록을 조회한다 (파티장이 맨 앞, N+1 방지)")
        void getParticipants_Success() {
            
            Long partyId = 1L;
            String userEmail = "test@example.com";

            PartyMember leader = PartyMember.createLeader(partyId, 1L);
            PartyMember member = PartyMember.createMember(partyId, 2L);

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(partyRepository.findById(partyId)).willReturn(Optional.of(testParty));
            given(partyMemberRepository.existsActiveMember(partyId, 1L)).willReturn(true);
            given(partyMemberRepository.findActiveMembers(partyId))
                    .willReturn(new ArrayList<>(List.of(member, leader)));
            given(userRepository.findAllById(anyList()))
                    .willReturn(new ArrayList<>(List.of(testUser, otherUser)));

            
            ParticipantListResponse response = chatRoomService.getParticipants(partyId, userEmail);

            
            assertThat(response).isNotNull();
            assertThat(response.partyId()).isEqualTo(1L);
            assertThat(response.participants()).hasSize(2);

            ParticipantListResponse.ParticipantInfo first = response.participants().get(0);
            assertThat(first.userId()).isEqualTo(1L);
            assertThat(first.isLeader()).isTrue();
            assertThat(first.nickname()).isEqualTo("테스터");

            ParticipantListResponse.ParticipantInfo second = response.participants().get(1);
            assertThat(second.userId()).isEqualTo(2L);
            assertThat(second.isLeader()).isFalse();
            assertThat(second.nickname()).isEqualTo("다른유저");

            then(userRepository).should().findAllById(anyList());
        }
    }

    @Nested
    @DisplayName("채팅 히스토리 조회 테스트")
    class GetChatHistoryTest {

        @Test
        @DisplayName("성공: 채팅 히스토리를 최신 순으로 반환하고 페이징 정보를 포함한다")
        void getChatHistory_Success() throws Exception {
            Long partyId = 1L;
            String userEmail = "test@example.com";

            ChatMessage oldMessage = ChatMessage.createChatMessage(partyId, 1L, "첫 번째 메시지");
            setId(oldMessage, 1L);
            setCreatedAt(oldMessage, LocalDateTime.now().minusMinutes(10));

            ChatMessage newMessage = ChatMessage.createChatMessage(partyId, 2L, "두 번째 메시지");
            setId(newMessage, 2L);
            setCreatedAt(newMessage, LocalDateTime.now().minusMinutes(5));

            Page<ChatMessage> messagePage = new PageImpl<>(
                    List.of(newMessage, oldMessage),  // ← 최신(2L)이 먼저
                    PageRequest.of(0, 20),
                    2
            );

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(partyMemberRepository.existsActiveMember(partyId, 1L)).willReturn(true);
            given(chatMessageRepository.findByPartyIdOrderByCreatedAtDesc(
                    eq(partyId), any(PageRequest.class)))
                    .willReturn(messagePage);
            given(userRepository.findAllById(anyList()))
                    .willReturn(List.of(testUser, otherUser));

            ChatHistoryResponse response = chatRoomService.getChatHistory(
                    partyId, 0, 20, userEmail);

            assertThat(response).isNotNull();
            assertThat(response.partyId()).isEqualTo(partyId);
            assertThat(response.messages()).hasSize(2);

            assertThat(response.currentPage()).isEqualTo(0);
            assertThat(response.totalPages()).isEqualTo(1);
            assertThat(response.totalMessages()).isEqualTo(2);
            assertThat(response.hasNext()).isFalse();

            assertThat(response.messages().get(0).messageId()).isEqualTo(2L);
            assertThat(response.messages().get(0).message()).isEqualTo("두 번째 메시지");

            assertThat(response.messages().get(1).messageId()).isEqualTo(1L);
            assertThat(response.messages().get(1).message()).isEqualTo("첫 번째 메시지");

            then(userRepository).should().findAllById(anyList());
        }

        @Test
        @DisplayName("성공: KICK 메시지의 메타데이터를 파싱한다")
        void getChatHistory_WithKickMessage() throws Exception {
            
            Long partyId = 1L;
            String userEmail = "test@example.com";

            ChatMessage kickMsg = ChatMessage.createKickMessage(partyId, 2L, 1L);
            setId(kickMsg, 1L);
            setCreatedAt(kickMsg, LocalDateTime.now());

            Page<ChatMessage> messagePage = new PageImpl<>(
                    List.of(kickMsg),
                    PageRequest.of(0, 20),
                    1
            );

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(partyMemberRepository.existsActiveMember(partyId, 1L)).willReturn(true);
            given(chatMessageRepository.findByPartyIdOrderByCreatedAtDesc(
                    eq(partyId), any(PageRequest.class)))
                    .willReturn(messagePage);
            given(userRepository.findAllById(anyList()))
                    .willReturn(List.of(testUser, otherUser));

            
            ChatHistoryResponse response = chatRoomService.getChatHistory(
                    partyId, 0, 20, userEmail);

            
            assertThat(response.messages()).hasSize(1);
            ChatHistoryResponse.ChatMessageDto msg = response.messages().get(0);
            assertThat(msg.type()).isEqualTo(MessageType.KICK);
            assertThat(msg.senderId()).isEqualTo(2L);
            assertThat(msg.kickedByLeaderId()).isEqualTo(1L);
            assertThat(msg.kickedByLeaderNickname()).isEqualTo("테스터");
        }

        @Test
        @DisplayName("성공: 메시지가 없으면 빈 리스트를 반환한다")
        void getChatHistory_Empty() {
            
            Long partyId = 1L;
            String userEmail = "test@example.com";

            Page<ChatMessage> emptyPage = new PageImpl<>(
                    Collections.emptyList(),
                    PageRequest.of(0, 20),
                    0
            );

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(partyMemberRepository.existsActiveMember(partyId, 1L)).willReturn(true);
            given(chatMessageRepository.findByPartyIdOrderByCreatedAtDesc(
                    eq(partyId), any(PageRequest.class)))
                    .willReturn(emptyPage);

            
            ChatHistoryResponse response = chatRoomService.getChatHistory(
                    partyId, 0, 20, userEmail);

            
            assertThat(response.messages()).isEmpty();
            assertThat(response.totalMessages()).isZero();
            assertThat(response.totalPages()).isZero();
        }
    }

    @Nested
    @DisplayName("내 채팅방 목록 조회 테스트")
    class GetMyChatRoomsTest {

        @Test
        @DisplayName("성공: 내 채팅방 목록을 최신 메시지 순으로 조회한다")
        void getMyChatRooms_Success() throws Exception {
            
            String userEmail = "test@example.com";

            PartyMember membership1 = PartyMember.createMember(1L, 1L);
            PartyMember membership2 = PartyMember.createMember(2L, 1L);

            Party party1 = Party.builder()
                    .scheduleId(1L)
                    .leaderId(2L)
                    .partyType(PartyType.LEAVE)
                    .partyName("파티1")
                    .departureLocation("강남역")
                    .arrivalLocation("잠실")
                    .transportType(TransportType.TAXI)
                    .maxMembers(4)
                    .preferredGender(Gender.ANY)
                    .preferredAge(PreferredAge.ANY)
                    .build();
            setId(party1, 1L);
            setCreatedAt(party1, LocalDateTime.now().minusDays(1));
            party1.incrementCurrentMembers(); // 2명

            Party party2 = Party.builder()
                    .scheduleId(2L)
                    .leaderId(3L)
                    .partyType(PartyType.LEAVE)
                    .partyName("파티2")
                    .departureLocation("신림역")
                    .arrivalLocation("홍대")
                    .transportType(TransportType.SUBWAY)
                    .maxMembers(5)
                    .preferredGender(Gender.ANY)
                    .preferredAge(PreferredAge.ANY)
                    .build();
            setId(party2, 2L);
            setCreatedAt(party2, LocalDateTime.now().minusDays(2));
            party2.incrementCurrentMembers(); // 2명

            ChatMessage lastMsg1 = ChatMessage.createChatMessage(1L, 1L, "파티1 메시지");
            setId(lastMsg1, 1L);
            setCreatedAt(lastMsg1, LocalDateTime.now().minusHours(1));

            ChatMessage lastMsg2 = ChatMessage.createChatMessage(2L, 1L, "파티2 메시지");
            setId(lastMsg2, 2L);
            setCreatedAt(lastMsg2, LocalDateTime.now().minusHours(2));

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(partyMemberRepository.findByUserId(1L))
                    .willReturn(new ArrayList<>(List.of(membership1, membership2)));
            given(partyRepository.findAllById(anyList()))
                    .willReturn(new ArrayList<>(List.of(party1, party2)));
            given(chatMessageRepository.findLastMessagesByPartyIds(anyList()))
                    .willReturn(new ArrayList<>(List.of(lastMsg1, lastMsg2)));

            
            MyChatRoomsResponse response = chatRoomService.getMyChatRooms(userEmail);

            
            assertThat(response).isNotNull();
            assertThat(response.chatRooms()).hasSize(2);
            assertThat(response.totalCount()).isEqualTo(2);

            MyChatRoomsResponse.ChatRoomItem first = response.chatRooms().get(0);
            assertThat(first.partyId()).isEqualTo(1L);
            assertThat(first.partyName()).isEqualTo("파티1");
            assertThat(first.participantCount()).isEqualTo(2);
            assertThat(first.lastMessage()).isEqualTo("파티1 메시지");

            MyChatRoomsResponse.ChatRoomItem second = response.chatRooms().get(1);
            assertThat(second.partyId()).isEqualTo(2L);
            assertThat(second.partyName()).isEqualTo("파티2");

            // N+1 방지 확인
            then(partyRepository).should().findAllById(anyList());
            then(chatMessageRepository).should().findLastMessagesByPartyIds(anyList());
        }

        @Test
        @DisplayName("성공: 1명인 파티는 목록에서 제외된다")
        void getMyChatRooms_ExcludeSingleMemberParty() throws Exception {
            
            String userEmail = "test@example.com";
            PartyMember membership = PartyMember.createMember(1L, 1L);

            Party singleParty = Party.builder()
                    .scheduleId(1L)
                    .leaderId(1L)
                    .partyType(PartyType.LEAVE)
                    .partyName("혼자인 파티")
                    .departureLocation("강남역")
                    .arrivalLocation("잠실")
                    .transportType(TransportType.TAXI)
                    .maxMembers(4)
                    .preferredGender(Gender.ANY)
                    .preferredAge(PreferredAge.ANY)
                    .build();
            setId(singleParty, 1L);
            setCreatedAt(singleParty, LocalDateTime.now()); // currentMembers = 1

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(partyMemberRepository.findByUserId(1L))
                    .willReturn(new ArrayList<>(List.of(membership)));
            given(partyRepository.findAllById(anyList()))
                    .willReturn(new ArrayList<>(List.of(singleParty)));

            
            MyChatRoomsResponse response = chatRoomService.getMyChatRooms(userEmail);

            
            assertThat(response.chatRooms()).isEmpty();
            assertThat(response.totalCount()).isZero();
        }

        @Test
        @DisplayName("성공: 마지막 메시지가 없으면 빈 문자열을 표시한다")
        void getMyChatRooms_NoLastMessage() throws Exception {
            
            String userEmail = "test@example.com";
            PartyMember membership = PartyMember.createMember(1L, 1L);

            Party party = Party.builder()
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
            setId(party, 1L);
            setCreatedAt(party, LocalDateTime.now());
            party.incrementCurrentMembers(); // 2명

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(partyMemberRepository.findByUserId(1L))
                    .willReturn(new ArrayList<>(List.of(membership)));
            given(partyRepository.findAllById(anyList()))
                    .willReturn(new ArrayList<>(List.of(party)));
            given(chatMessageRepository.findLastMessagesByPartyIds(anyList()))
                    .willReturn(Collections.emptyList());

            
            MyChatRoomsResponse response = chatRoomService.getMyChatRooms(userEmail);

            
            assertThat(response.chatRooms()).hasSize(1);
            MyChatRoomsResponse.ChatRoomItem room = response.chatRooms().get(0);
            assertThat(room.lastMessage()).isEmpty();
            assertThat(room.lastMessageTime()).isEqualTo(party.getCreatedAt());
        }

        @Test
        @DisplayName("성공: 메시지 타입별 마지막 메시지를 적절히 포맷팅한다")
        void getMyChatRooms_MessageTypeFormatting() throws Exception {
            
            String userEmail = "test@example.com";

            PartyMember membership1 = PartyMember.createMember(1L, 1L);
            PartyMember membership2 = PartyMember.createMember(2L, 1L);
            PartyMember membership3 = PartyMember.createMember(3L, 1L);

            Party party1 = createPartyWithMembers(1L, "파티1");
            Party party2 = createPartyWithMembers(2L, "파티2");
            Party party3 = createPartyWithMembers(3L, "파티3");

            ChatMessage joinMsg = ChatMessage.createJoinMessage(1L, 1L);
            setId(joinMsg, 1L);
            setCreatedAt(joinMsg, LocalDateTime.now());

            ChatMessage leaveMsg = ChatMessage.createLeaveMessage(2L, 1L);
            setId(leaveMsg, 2L);
            setCreatedAt(leaveMsg, LocalDateTime.now());

            ChatMessage kickMsg = ChatMessage.createKickMessage(3L, 2L, 1L);
            setId(kickMsg, 3L);
            setCreatedAt(kickMsg, LocalDateTime.now());

            given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
            given(partyMemberRepository.findByUserId(1L))
                    .willReturn(new ArrayList<>(List.of(membership1, membership2, membership3)));
            given(partyRepository.findAllById(anyList()))
                    .willReturn(new ArrayList<>(List.of(party1, party2, party3)));
            given(chatMessageRepository.findLastMessagesByPartyIds(anyList()))
                    .willReturn(new ArrayList<>(List.of(joinMsg, leaveMsg, kickMsg)));
            
            MyChatRoomsResponse response = chatRoomService.getMyChatRooms(userEmail);
            
            Map<Long, String> lastMessages = response.chatRooms().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            MyChatRoomsResponse.ChatRoomItem::partyId,
                            MyChatRoomsResponse.ChatRoomItem::lastMessage
                    ));

            assertThat(lastMessages.get(1L)).isEqualTo("입장하셨습니다");
            assertThat(lastMessages.get(2L)).isEqualTo("퇴장하셨습니다");
            assertThat(lastMessages.get(3L)).isEqualTo("강퇴되었습니다");
        }

        private Party createPartyWithMembers(Long partyId, String partyName) throws Exception {
            Party party = Party.builder()
                    .scheduleId(partyId)
                    .leaderId(1L)
                    .partyType(PartyType.LEAVE)
                    .partyName(partyName)
                    .departureLocation("강남역")
                    .arrivalLocation("잠실")
                    .transportType(TransportType.TAXI)
                    .maxMembers(4)
                    .preferredGender(Gender.ANY)
                    .preferredAge(PreferredAge.ANY)
                    .build();
            setId(party, partyId);
            setCreatedAt(party, LocalDateTime.now());
            party.incrementCurrentMembers(); // 2명
            return party;
        }
    }
}