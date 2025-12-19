package back.kalender.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "채팅방 정보 응답")
public record ChatRoomInfoResponse(

        @Schema(description = "파티 ID", example = "1")
        Long partyId,

        @Schema(description = "파티 이름", example = "지민이 최애")
        String partyName,

        @Schema(description = "참여자 수", example = "3")
        Integer participantCount,

        @Schema(description = "최대 인원", example = "5")
        Integer maxParticipants,

        @Schema(description = "활성화 여부", example = "true")
        Boolean isActive,

        @Schema(description = "생성 시간", example = "2024-12-15T10:00:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {}