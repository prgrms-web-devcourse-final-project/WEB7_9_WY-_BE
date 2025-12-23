package back.kalender.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "프로필 이미지 presigned PUT 발급 응답")
public record PresignProfileImageResponse(
        String key,
        String uploadUrl,
        Map<String, String> requiredHeaders,
        long expiresInSeconds
) {}