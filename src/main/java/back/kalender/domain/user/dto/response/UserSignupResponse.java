package back.kalender.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "회원가입 응답")
public record UserSignupResponse(
        Long userId,
        String email,
        String nickname,
        LocalDateTime createdAt
) {}





