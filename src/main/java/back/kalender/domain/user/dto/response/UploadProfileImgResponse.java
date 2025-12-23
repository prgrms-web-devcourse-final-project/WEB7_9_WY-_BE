package back.kalender.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로필 이미지 업로드 완료 응답")
public record UploadProfileImgResponse (
        String profileImageUrl
) {}
