package back.kalender.domain.user.dto.response;

import back.kalender.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "회원가입 응답")
public record UserSignupResponse(
        Long userId,
        String email,
        String nickname,
        LocalDate birthDate,
        LocalDateTime createdAt
) {
    public static UserSignupResponse from(User user) {
        return new UserSignupResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getBirthDate(),
                user.getCreatedAt()
        );
    }
}





