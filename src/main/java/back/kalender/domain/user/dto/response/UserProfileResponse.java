package back.kalender.domain.user.dto.response;

import back.kalender.domain.user.entity.User;
import back.kalender.global.common.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 프로필 응답")
public record UserProfileResponse(
        Long userId,
        String email,
        String nickname,
        String profileImage,
        Integer level,
        Integer age,
        Gender gender
) {
    public static UserProfileResponse from(User user, String profileImageUrl) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                profileImageUrl,
                user.getLevel(),
                user.getAge(),
                user.getGender()
        );
    }
}
