package back.kalender.domain.chat.service;

import back.kalender.domain.chat.dto.request.SendMessageRequest;
import back.kalender.domain.chat.dto.response.ChatMessageResponse;
import back.kalender.domain.chat.dto.response.KickMemberResponse;
import back.kalender.domain.chat.dto.response.LeaveRoomResponse;
import back.kalender.domain.chat.dto.response.RoomJoinedResponse;
import back.kalender.domain.chat.entity.ChatMessage;
import back.kalender.domain.chat.enums.MessageType;
import back.kalender.domain.chat.repository.ChatMessageRepository;
import back.kalender.domain.chat.repository.ChatRoomRepository;
import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.entity.PartyMember;
import back.kalender.domain.party.repository.PartyMemberRepository;
import back.kalender.domain.party.repository.PartyRepository;
import back.kalender.domain.party.service.PartyService;
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatService {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PartyService partyService;

    private User validateUserAndPartyMember(Long partyId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        if (!partyMemberRepository.existsActiveMember(partyId, user.getId())) {
            throw new ServiceException(ErrorCode.UNAUTHORIZED_PARTY_ACCESS);
        }

        return user;
    }

    private Party validateParty(Long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PARTY_NOT_FOUND));
    }

    private PartyMember getActiveMember(Long partyId, Long userId) {
        return partyMemberRepository
                .findByPartyIdAndUserIdAndLeftAtIsNull(partyId, userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_IN_PARTY));
    }

    @Transactional
    public RoomJoinedResponse joinRoom(Long partyId, String userEmail) {
        User user = validateUserAndPartyMember(partyId, userEmail);
        Party party = validateParty(partyId);

        chatRoomRepository.findActiveByPartyId(partyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.CHAT_ROOM_NOT_ACTIVE));

        ChatMessage joinMessage = ChatMessage.createJoinMessage(partyId, user.getId());
        chatMessageRepository.save(joinMessage);

        return new RoomJoinedResponse(
                MessageType.JOIN,
                partyId,
                user.getId(),
                user.getNickname(),
                user.getProfileImage(),
                user.getNickname() + "님이 입장하셨습니다",
                joinMessage.getCreatedAt(),
                party.getCurrentMembers()
        );
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long partyId, SendMessageRequest request, String userEmail) {
        User user = validateUserAndPartyMember(partyId, userEmail);

        ChatMessage chatMessage = ChatMessage.createChatMessage(
                partyId, user.getId(), request.message());
        chatMessageRepository.save(chatMessage);

        return new ChatMessageResponse(
                MessageType.CHAT,
                partyId,
                user.getId(),
                user.getNickname(),
                user.getProfileImage(),
                request.message(),
                chatMessage.getCreatedAt()
        );
    }

    @Transactional
    public LeaveRoomResponse leaveRoom(Long partyId, String userEmail) {
        User user = validateUserAndPartyMember(partyId, userEmail);
        Party party = validateParty(partyId);

        if (party.isLeader(user.getId())) {
            throw new ServiceException(ErrorCode.LEADER_CANNOT_LEAVE);
        }

        partyService.removePartyMember(partyId, user.getId());

        ChatMessage leaveMessage = ChatMessage.createLeaveMessage(partyId, user.getId());
        chatMessageRepository.save(leaveMessage);

        Party updatedParty = validateParty(partyId);

        return new LeaveRoomResponse(
                MessageType.LEAVE,
                partyId,
                user.getId(),
                user.getNickname(),
                user.getNickname() + "님이 퇴장하셨습니다",
                leaveMessage.getCreatedAt(),
                updatedParty.getCurrentMembers()
        );
    }

    @Transactional
    public KickMemberResponse kickMember(Long partyId, Long targetMemberId, String userEmail) {
        User leader = validateUserAndPartyMember(partyId, userEmail);
        Party party = validateParty(partyId);

        if (!party.isLeader(leader.getId())) {
            throw new ServiceException(ErrorCode.ONLY_LEADER_CAN_KICK);
        }

        if (leader.getId().equals(targetMemberId)) {
            throw new ServiceException(ErrorCode.CANNOT_KICK_YOURSELF);
        }

        User targetMember = userRepository.findById(targetMemberId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        if (!partyMemberRepository.existsActiveMember(partyId, targetMemberId)) {
            throw new ServiceException(ErrorCode.USER_NOT_IN_PARTY);
        }

        partyService.kickPartyMember(partyId, targetMemberId);

        ChatMessage kickMessage = ChatMessage.createKickMessage(
                partyId, targetMemberId, leader.getId());
        chatMessageRepository.save(kickMessage);

        Party updatedParty = validateParty(partyId);

        return new KickMemberResponse(
                MessageType.KICK,
                partyId,
                targetMemberId,
                targetMember.getNickname(),
                leader.getId(),
                leader.getNickname(),
                targetMember.getNickname() + "님이 강퇴되었습니다",
                kickMessage.getCreatedAt(),
                updatedParty.getCurrentMembers()
        );
    }
}