package back.kalender.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 요청")
public record UserSignupRequest (
        String email,
        String password,
        String nickname,
        String gender
){
}
