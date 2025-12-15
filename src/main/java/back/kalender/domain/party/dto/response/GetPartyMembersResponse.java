package back.kalender.domain.party.dto.response;

import back.kalender.domain.party.entity.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "파티 확정 멤버 목록 조회 응답")
public record GetPartyMembersResponse(

        @Schema(description = "파티 ID", example = "1")
        Long partyId,

        @Schema(description = "멤버 목록")
        List<MemberInfo> members,

        @Schema(description = "총 멤버 수", example = "2")
        Integer totalMembers
) {

    @Schema(description = "멤버 정보")
    public record MemberInfo(

            @Schema(description = "멤버 ID", example = "1")
            Long memberId,

            @Schema(description = "사용자 ID", example = "100")
            Long userId,

            @Schema(description = "닉네임", example = "지민이 최애")
            String nickname,

            @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
            String profileImage,

            @Schema(description = "역할", example = "파티장", allowableValues = {"파티장", "멤버"})
            MemberRole role
    ) {}
}