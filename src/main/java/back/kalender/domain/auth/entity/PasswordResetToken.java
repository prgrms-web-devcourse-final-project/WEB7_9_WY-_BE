package back.kalender.domain.auth.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 비밀번호 변경
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "password_reset_tokens",
        indexes = {
                @Index(name = "idx_password_user_id", columnList = "userId"),
                @Index(name = "idx_password_token", columnList = "token")
        }
)
public class PasswordResetToken extends BaseEntity {

    private Long userId;

    @Column(nullable = false, length = 100)
    private String token;

    private boolean used;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    public static PasswordResetToken create(Long userId, String code) {
        PasswordResetToken token = new PasswordResetToken();
        token.userId = userId;
        token.token = code;
        token.used = false;
        token.expiredAt = LocalDateTime.now().plusMinutes(5);
        return token;
    }

    public void markUsed() {
        this.used = true;
    }
}