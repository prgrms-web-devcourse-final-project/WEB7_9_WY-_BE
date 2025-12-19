package back.kalender.domain.chat.entity;

import back.kalender.domain.chat.enums.MessageType;
import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "chat_messages",
        indexes = @Index(name = "idx_party_created", columnList = "party_id, created_at DESC")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @Column(nullable = false)
    private Long partyId;

    @Column(nullable = false)
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType messageType;

    @Column(length = 500)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Builder
    private ChatMessage(Long partyId, Long senderId, MessageType messageType,
                        String content, String metadata) {
        this.partyId = partyId;
        this.senderId = senderId;
        this.messageType = messageType;
        this.content = content;
        this.metadata = metadata;
    }

    public static ChatMessage createChatMessage(Long partyId, Long senderId, String content) {
        return ChatMessage.builder()
                .partyId(partyId)
                .senderId(senderId)
                .messageType(MessageType.CHAT)
                .content(content)
                .build();
    }

    public static ChatMessage createJoinMessage(Long partyId, Long userId) {
        return ChatMessage.builder()
                .partyId(partyId)
                .senderId(userId)
                .messageType(MessageType.JOIN)
                .build();
    }

    public static ChatMessage createLeaveMessage(Long partyId, Long userId) {
        return ChatMessage.builder()
                .partyId(partyId)
                .senderId(userId)
                .messageType(MessageType.LEAVE)
                .build();
    }

    public static ChatMessage createKickMessage(Long partyId, Long kickedUserId,
                                                Long kickedByLeaderId) {
        return ChatMessage.builder()
                .partyId(partyId)
                .senderId(kickedUserId)
                .messageType(MessageType.KICK)
                .metadata(String.format("{\"kickedBy\":%d}", kickedByLeaderId))
                .build();
    }
}