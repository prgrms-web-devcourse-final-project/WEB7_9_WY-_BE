package back.kalender.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "비밀번호 재설정 요청")
public record UserPasswordResetRequest(
        @Schema(description = "비밀번호 재설정 토큰", example = "reset-token-12345-test-reset-token", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "토큰을 입력해주세요")
        String token,

        @Schema(description = "새 비밀번호", example = "newPassword123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "새 비밀번호를 입력해주세요")
        String newPassword,

        @Schema(description = "새 비밀번호 확인", example = "newPassword123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "새 비밀번호 확인을 입력해주세요")
        String newPasswordConfirm
) {
}
