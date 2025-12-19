package back.kalender.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "채팅방 참여자 목록 응답")
public record ParticipantListResponse(

        @Schema(description = "파티 ID", example = "1")
        Long partyId,

        @Schema(description = "참여자 목록")
        List<ParticipantInfo> participants
) {
    @Schema(description = "참여자 정보")
    public record ParticipantInfo(

            @Schema(description = "사용자 ID", example = "5")
            Long userId,

            @Schema(description = "닉네임", example = "팬덤러버")
            String nickname,

            @Schema(description = "프로필 이미지", example = "https://example.com/profile.jpg")
            String profileImage,

            @Schema(description = "파티장 여부", example = "false")
            Boolean isLeader,

            @Schema(description = "온라인 상태", example = "false")
            Boolean isOnline
    ) {}
}