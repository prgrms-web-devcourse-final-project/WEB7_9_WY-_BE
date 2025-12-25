package back.kalender.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로필 이미지 업로드 완료 요청")
public record CompleteProfileImageRequest(
        String Key
) {
}
