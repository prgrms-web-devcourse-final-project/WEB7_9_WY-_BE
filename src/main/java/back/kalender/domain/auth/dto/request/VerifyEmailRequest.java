package back.kalender.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "이메일 인증 확인 요청")
public record VerifyEmailRequest(
        @Schema(description = "이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @Email(message = "이메일 형식이여야 합니다.")
        @NotBlank(message = "이메일을 입력해주세요")
        String email,

        @Schema(description = "인증 코드", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "코드를 입력해주세요")
        String code
) {
}
