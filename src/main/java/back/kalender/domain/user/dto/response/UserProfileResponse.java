package back.kalender.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 프로필 응답")
public record UserProfileResponse(
        Long userId,
        String email,
        String nickname,
        String profileImage,
        Integer level
) {}
