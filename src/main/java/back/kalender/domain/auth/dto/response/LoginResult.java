package back.kalender.domain.auth.dto.response;

public record LoginResult(
        UserLoginResponse loginResponse,
        String accessToken
) {
}

