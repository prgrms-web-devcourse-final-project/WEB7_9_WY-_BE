package back.kalender.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = """
                유저 로그인 응답
                
                **응답 정보:**
                - Response Body에는 유저 정보만 포함됩니다
                - access token은 Response Header의 Authorization에 'Bearer {token}' 형식으로 설정됩니다
                - refresh token은 httpOnly secure 쿠키로 자동 설정됩니다
                - 쿠키 이름: refreshToken
                """
)
public record UserLoginResponse(
        @Schema(description = "유저 ID", example = "1")
        Long userId,

        @Schema(description = "닉네임", example = "홍길동")
        String nickname,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        String profileImage,

        @Schema(description = "이메일 인증 여부", example = "true")
        Boolean emailVerified
) {
}
