package back.kalender.domain.auth.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 이메일 인증
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "email_verifications",
        indexes = {
                @Index(name = "idx_user_id", columnList = "userId"),
                @Index(name = "idx_code", columnList = "code")
        }
)
public class EmailVerification extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String code;

    private boolean used;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    public static EmailVerification create(Long userId, String code) {
        EmailVerification ev = new EmailVerification();
        ev.userId = userId;
        ev.code = code;
        ev.used = false;
        ev.expiredAt = LocalDateTime.now().plusMinutes(5);
        return ev;
    }

    public void markUsed() {
        this.used = true;
    }
}
