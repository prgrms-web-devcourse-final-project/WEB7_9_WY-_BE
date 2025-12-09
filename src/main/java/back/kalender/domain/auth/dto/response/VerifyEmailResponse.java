package back.kalender.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 인증 확인 응답")
public record VerifyEmailResponse(
        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "이메일 인증 여부", example = "true")
        Boolean emailVerified
) {
}
