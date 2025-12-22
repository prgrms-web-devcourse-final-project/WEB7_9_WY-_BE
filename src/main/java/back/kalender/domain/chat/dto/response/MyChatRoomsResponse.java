package back.kalender.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "내 채팅방 목록 응답")
public record MyChatRoomsResponse(

        @Schema(description = "채팅방 목록")
        List<ChatRoomItem> chatRooms,

        @Schema(description = "전체 개수", example = "2")
        Integer totalCount
) {
    @Schema(description = "채팅방 항목")
    public record ChatRoomItem(

            @Schema(description = "파티 ID", example = "1")
            Long partyId,

            @Schema(description = "파티 이름", example = "지민이 최애")
            String partyName,

            @Schema(description = "참여자 수", example = "3")
            Integer participantCount,

            @Schema(description = "마지막 메시지", example = "안녕하세요!")
            String lastMessage,

            @Schema(description = "마지막 메시지 시간", example = "2024-12-16T14:30:00")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime lastMessageTime,

            @Schema(description = "읽지 않은 메시지 수", example = "0")
            Integer unreadCount
    ) {}
}