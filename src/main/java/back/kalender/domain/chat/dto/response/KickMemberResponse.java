package back.kalender.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "멤버 강퇴 응답")
public record KickMemberResponse(

        @Schema(description = "메시지 타입 (항상 KICK)", example = "KICK")
        String type,

        @Schema(description = "파티 ID", example = "1")
        Long partyId,

        @Schema(description = "강퇴된 멤버 ID", example = "3")
        Long kickedMemberId,

        @Schema(description = "강퇴된 멤버 닉네임", example = "문제유저")
        String kickedMemberNickname,

        @Schema(description = "강퇴 실행한 파티장 ID", example = "1")
        Long kickedByLeaderId,

        @Schema(description = "강퇴 실행한 파티장 닉네임", example = "파티장님")
        String kickedByLeaderNickname,

        @Schema(description = "강퇴 메시지", example = "문제유저님이 강퇴되었습니다")
        String message,

        @Schema(description = "강퇴 시간", example = "2024-12-16T14:40:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,

        @Schema(description = "현재 채팅방 참여자 수", example = "3")
        Integer participantCount
) {
}