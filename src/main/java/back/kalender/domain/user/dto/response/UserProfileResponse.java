package back.kalender.domain.user.dto.response;

import back.kalender.global.common.Enum.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 프로필 응답")
public record UserProfileResponse(
        String email,
        String nickname,
        String profileImage,
        Integer level,
        Integer age,
        Gender gender
) {}
