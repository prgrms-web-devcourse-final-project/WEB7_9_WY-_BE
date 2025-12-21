package back.kalender.domain.chat.dto.response;

import back.kalender.domain.chat.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "채팅방 퇴장 응답")
public record LeaveRoomResponse(

        @Schema(description = "메시지 타입 (항상 LEAVE)", example = "LEAVE")
        MessageType type,

        @Schema(description = "파티 ID", example = "1")
        Long partyId,

        @Schema(description = "퇴장한 사용자 ID", example = "5")
        Long userId,

        @Schema(description = "퇴장한 사용자 닉네임", example = "팬덤러버")
        String userNickname,

        @Schema(description = "퇴장 메시지", example = "팬덤러버님이 퇴장하셨습니다")
        String message,

        @Schema(description = "퇴장 시간", example = "2024-12-16T14:35:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,

        @Schema(description = "현재 채팅방 참여자 수", example = "3")
        Integer participantCount
) {
}