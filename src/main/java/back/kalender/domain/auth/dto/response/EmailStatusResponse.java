package back.kalender.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "이메일 인증 상태 응답")
public record EmailStatusResponse(
        @Schema(description = "유저 ID", example = "1")
        Long userId,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "이메일 인증 여부", example = "true")
        Boolean emailVerified,

        @Schema(description = "인증 완료 시간", example = "2024-01-01T12:00:00")
        LocalDateTime verifiedAt
) {
}
