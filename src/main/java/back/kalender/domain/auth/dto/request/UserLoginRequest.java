package back.kalender.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "유저 로그인 요청")
public record UserLoginRequest(
        @Schema(description = "이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @Email(message = "이메일 형식이여야 합니다.")
        @NotBlank(message = "이메일을 입력해주세요")
        String email,

        @Schema(description = "비밀번호", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "비밀번호를 입력해주세요")
        String password
) {}