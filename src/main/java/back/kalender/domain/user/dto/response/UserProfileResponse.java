package back.kalender.domain.user.dto.response;

import back.kalender.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 프로필 응답")
public record UserProfileResponse(
        String email,
        String nickname,
        String profileImage,
        Integer level,
        Integer age,
        String gender
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getEmail(),
                user.getNickname(),
                user.getProfileImage(),
                user.getLevel(),
                user.getAge(),
                user.getGender()
        );
    }
}
