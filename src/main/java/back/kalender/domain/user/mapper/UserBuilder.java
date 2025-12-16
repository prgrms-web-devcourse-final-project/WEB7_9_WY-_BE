package back.kalender.domain.user.mapper;

import back.kalender.domain.user.dto.request.UserSignupRequest;
import back.kalender.domain.user.entity.User;
import back.kalender.global.common.Enum.Gender;

public class UserBuilder {
    public static User create(UserSignupRequest request, String encodedPassword, Gender gender) {
        return User.builder()
                .email(request.email())
                .password(encodedPassword)
                .nickname(request.nickname())
                .gender(gender)
                .birthDate(request.birthDate())
                .emailVerified(false)
                .build();
    }
}

