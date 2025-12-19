package back.kalender.domain.chat.dto.response;

import back.kalender.domain.chat.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "채팅 히스토리 응답")
public record ChatHistoryResponse(

        @Schema(description = "파티 ID", example = "1")
        Long partyId,

        @Schema(description = "메시지 목록")
        List<ChatMessageDto> messages,

        @Schema(description = "현재 페이지", example = "0")
        int currentPage,

        @Schema(description = "전체 페이지 수", example = "5")
        int totalPages,

        @Schema(description = "전체 메시지 수", example = "245")
        long totalMessages,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
    @Schema(description = "채팅 메시지")
    public record ChatMessageDto(
            @Schema(description = "메시지 ID", example = "1")
            Long messageId,

            @Schema(description = "메시지 타입", implementation = MessageType.class)
            MessageType type,

            @Schema(description = "발신자 ID", example = "5")
            Long senderId,

            @Schema(description = "발신자 닉네임", example = "팬덤러버")
            String senderNickname,

            @Schema(description = "발신자 프로필 이미지", example = "https://example.com/profile.jpg")
            String senderProfileImage,

            @Schema(description = "메시지 내용", example = "안녕하세요!")
            String message,

            @Schema(description = "전송 시간", example = "2024-12-16T14:30:00")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime timestamp,

            @Schema(description = "강퇴한 파티장 ID (KICK 타입만)", example = "1")
            Long kickedByLeaderId,

            @Schema(description = "강퇴한 파티장 닉네임 (KICK 타입만)", example = "파티장님")
            String kickedByLeaderNickname
    ) {}
}