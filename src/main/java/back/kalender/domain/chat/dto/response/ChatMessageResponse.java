package back.kalender.domain.chat.dto.response;

import back.kalender.domain.chat.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "채팅 메시지 응답")
public record ChatMessageResponse(

        @Schema(description = "메시지 타입 (CHAT, JOIN, LEAVE, KICK)", example = "CHAT")
        MessageType type,

        @Schema(description = "파티 ID", example = "1")
        Long partyId,

        @Schema(description = "발신자 ID", example = "5")
        Long senderId,

        @Schema(description = "발신자 닉네임", example = "팬덤러버")
        String senderNickname,

        @Schema(description = "발신자 프로필 이미지 URL", example = "https://example.com/profile.jpg")
        String senderProfileImage,

        @Schema(description = "메시지 내용", example = "안녕하세요!")
        String message,

        @Schema(description = "메시지 전송 시간", example = "2024-12-16T14:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {}