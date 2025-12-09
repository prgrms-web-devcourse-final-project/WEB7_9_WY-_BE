package back.kalender.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 정보 수정 요청")
public record UpdateProfileRequest (
        String nickname,
        String profileImage,
        String password,
        String currentPassword
){
}
