package back.kalender.domain.chat.service;

import back.kalender.domain.chat.dto.response.ChatHistoryResponse;
import back.kalender.domain.chat.dto.response.ChatRoomInfoResponse;
import back.kalender.domain.chat.dto.response.MyChatRoomsResponse;
import back.kalender.domain.chat.dto.response.ParticipantListResponse;
import back.kalender.domain.chat.entity.ChatMessage;
import back.kalender.domain.chat.entity.ChatRoom;
import back.kalender.domain.chat.enums.MessageType;
import back.kalender.domain.chat.repository.ChatMessageRepository;
import back.kalender.domain.chat.repository.ChatRoomRepository;
import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.entity.PartyMember;
import back.kalender.domain.party.repository.PartyMemberRepository;
import back.kalender.domain.party.repository.PartyRepository;
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void createChatRoom(Long partyId, String partyName) {
        if (chatRoomRepository.existsByPartyId(partyId)) {
            log.warn("채팅방이 이미 존재합니다 - partyId: {}", partyId);
            return;
        }

        ChatRoom chatRoom = ChatRoom.create(partyId, partyName);
        chatRoomRepository.save(chatRoom);

        log.info("채팅방 생성 완료 - partyId: {}, roomName: {}", partyId, partyName);
    }

    @Transactional
    public void closeChatRoom(Long partyId) {
        ChatRoom chatRoom = chatRoomRepository.findByPartyId(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        chatRoom.deactivate();
        log.info("채팅방 종료 - partyId: {}", partyId);
    }

    public ChatRoomInfoResponse getChatRoomInfo(Long partyId, String userEmail) {
        log.info("채팅방 정보 조회 - partyId: {}, userEmail: {}", partyId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        if (!partyMemberRepository.existsActiveMember(partyId, user.getId())) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED_PARTY_ACCESS);
        }

        ChatRoom chatRoom = chatRoomRepository.findByPartyId(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        return new ChatRoomInfoResponse(
                party.getId(),
                party.getPartyName(),
                party.getCurrentMembers(),
                party.getMaxMembers(),
                chatRoom.getIsActive(),
                chatRoom.getCreatedAt()
        );
    }

    public ParticipantListResponse getParticipants(Long partyId, String userEmail) {
        log.info("참여자 목록 조회 - partyId: {}, userEmail: {}", partyId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));

        if (!partyMemberRepository.existsActiveMember(partyId, user.getId())) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED_PARTY_ACCESS);
        }

        List<PartyMember> members = partyMemberRepository.findActiveMembers(partyId);

        List<Long> userIds = members.stream()
                .map(PartyMember::getUserId)
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<ParticipantListResponse.ParticipantInfo> participantInfos = members.stream()
                .map(member -> {
                    User memberUser = userMap.get(member.getUserId());
                    if (memberUser == null) {
                        throw new ServiceException(ErrorCode.USER_NOT_FOUND);
                    }

                    boolean isLeader = party.getLeaderId().equals(member.getUserId());

                    return new ParticipantListResponse.ParticipantInfo(
                            memberUser.getId(),
                            memberUser.getNickname(),
                            memberUser.getProfileImage(),
                            isLeader,
                            false
                    );
                })
                .sorted((a, b) -> {
                    if (a.isLeader() && !b.isLeader()) return -1;
                    if (!a.isLeader() && b.isLeader()) return 1;
                    return 0;
                })
                .toList();

        return new ParticipantListResponse(partyId, participantInfos);
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistory(
            Long partyId, int page, int size, String userEmail) {

        log.info("채팅 히스토리 조회 - partyId: {}, page: {}, size: {}, userEmail: {}",
                partyId, page, size, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        if (!partyMemberRepository.existsActiveMember(partyId, user.getId())) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED_PARTY_ACCESS);
        }

        Page<ChatMessage> messagePage = chatMessageRepository
                .findByPartyIdOrderByCreatedAtDesc(partyId, PageRequest.of(page, size));

        List<ChatMessage> messages = messagePage.getContent();

        if (messages.isEmpty()) {
            return new ChatHistoryResponse(
                    partyId,
                    Collections.emptyList(),
                    messagePage.getNumber(),
                    messagePage.getTotalPages(),
                    messagePage.getTotalElements(),
                    messagePage.hasNext()
            );
        }

        List<Long> senderIds = messages.stream()
                .map(ChatMessage::getSenderId)
                .collect(Collectors.toCollection(ArrayList::new));

        Set<Long> leaderIds = messages.stream()
                .filter(msg -> msg.getMessageType() == MessageType.KICK)
                .filter(msg -> msg.getMetadata() != null)
                .map(this::extractLeaderIdFromMetadata)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> allUserIds = new HashSet<>(senderIds);
        allUserIds.addAll(leaderIds);

        Map<Long, User> userMap = userRepository.findAllById(new ArrayList<>(allUserIds)).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<ChatHistoryResponse.ChatMessageDto> messageDtos =
                new ArrayList<>(
                        messages.stream()
                                .map(msg -> convertToDto(msg, userMap))
                                .toList()
                );

        Collections.reverse(messageDtos);

        log.info("채팅 히스토리 조회 완료 - partyId: {}, 메시지 수: {}", partyId, messageDtos.size());

        return new ChatHistoryResponse(
                partyId,
                messageDtos,
                messagePage.getNumber(),
                messagePage.getTotalPages(),
                messagePage.getTotalElements(),
                messagePage.hasNext()
        );
    }

    private ChatHistoryResponse.ChatMessageDto convertToDto(
            ChatMessage message, Map<Long, User> userMap) {

        User sender = userMap.get(message.getSenderId());
        if (sender == null) {
            log.warn("발신자 정보 없음 - senderId: {}", message.getSenderId());
            throw new ServiceException(ErrorCode.USER_NOT_FOUND);
        }

        Long kickedByLeaderId = null;
        String kickedByLeaderNickname = null;

        if (message.getMessageType() == MessageType.KICK && message.getMetadata() != null) {
            kickedByLeaderId = extractLeaderIdFromMetadata(message);
            if (kickedByLeaderId != null) {
                User leader = userMap.get(kickedByLeaderId);
                if (leader != null) {
                    kickedByLeaderNickname = leader.getNickname();
                }
            }
        }

        return new ChatHistoryResponse.ChatMessageDto(
                message.getId(),
                message.getMessageType(),
                message.getSenderId(),
                sender.getNickname(),
                sender.getProfileImage(),
                message.getContent(),
                message.getCreatedAt(),
                kickedByLeaderId,
                kickedByLeaderNickname
        );
    }

    private Long extractLeaderIdFromMetadata(ChatMessage message) {
        if (message.getMetadata() == null) {
            return null;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(message.getMetadata());
            if (jsonNode.has("kickedBy")) {
                return jsonNode.get("kickedBy").asLong();
            }
        } catch (JsonProcessingException e) {
            log.warn("metadata 파싱 실패 - messageId: {}, metadata: {}",
                    message.getId(), message.getMetadata());
        }

        return null;
    }

    public MyChatRoomsResponse getMyChatRooms(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        List<PartyMember> myMemberships = partyMemberRepository.findByUserId(user.getId());

        List<Long> partyIds = myMemberships.stream()
                .map(PartyMember::getPartyId)
                .toList();

        if (partyIds.isEmpty()) {
            return new MyChatRoomsResponse(Collections.emptyList(), 0);
        }

        Map<Long, Party> partyMap = partyRepository.findAllById(partyIds).stream()
                .collect(Collectors.toMap(Party::getId, p -> p));

        List<Long> validPartyIds = partyIds.stream()
                .filter(partyId -> {
                    Party party = partyMap.get(partyId);
                    return party != null && party.getCurrentMembers() >= 2;
                })
                .toList();

        if (validPartyIds.isEmpty()) {
            return new MyChatRoomsResponse(Collections.emptyList(), 0);
        }

        List<ChatMessage> lastMessages = chatMessageRepository
                .findLastMessagesByPartyIds(validPartyIds);

        Map<Long, ChatMessage> lastMessageMap = lastMessages.stream()
                .collect(Collectors.toMap(
                        ChatMessage::getPartyId,
                        msg -> msg
                ));

        List<MyChatRoomsResponse.ChatRoomItem> chatRoomItems = validPartyIds.stream()
                .map(partyId -> {
                    Party party = partyMap.get(partyId);

                    ChatMessage lastMessage = lastMessageMap.get(partyId);

                    String lastMessageText = "";
                    LocalDateTime lastMessageTime = party.getCreatedAt();

                    if (lastMessage != null) {
                        lastMessageText = formatLastMessage(lastMessage);
                        lastMessageTime = lastMessage.getCreatedAt();
                    }

                    return new MyChatRoomsResponse.ChatRoomItem(
                            partyId,
                            party.getPartyName(),
                            party.getCurrentMembers(),
                            lastMessageText,
                            lastMessageTime,
                            0
                    );
                })
                .sorted((a, b) -> b.lastMessageTime().compareTo(a.lastMessageTime()))
                .toList();

        return new MyChatRoomsResponse(chatRoomItems, chatRoomItems.size());
    }

    private String formatLastMessage(ChatMessage message) {
        return switch (message.getMessageType()) {
            case CHAT -> message.getContent();
            case JOIN -> "입장하셨습니다";
            case LEAVE -> "퇴장하셨습니다";
            case KICK -> "강퇴되었습니다";
        };
    }
}