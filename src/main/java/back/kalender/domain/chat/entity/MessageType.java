package back.kalender.domain.chat.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메시지 타입 Enum")
public enum MessageType {
    @Schema(description = "일반 채팅 메시지")
    CHAT,

    @Schema(description = "입장 메시지")
    JOIN,

    @Schema(description = "퇴장 메시지")
    LEAVE,

    @Schema(description = "강퇴 메시지")
    KICK
}
