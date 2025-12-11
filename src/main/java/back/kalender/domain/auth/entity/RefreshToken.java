package back.kalender.domain.auth.entity;

import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 리프레시 토큰
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_user_id", columnList = "userId"),
                @Index(name = "idx_refresh_token", columnList = "token")
        }
)
public class RefreshToken extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 1000)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    public static RefreshToken create(Long userId, String token, long ttlDays) {
        RefreshToken rt = new RefreshToken();
        rt.userId = userId;
        rt.token = token;
        rt.expiredAt = LocalDateTime.now().plusDays(ttlDays);
        return rt;
    }

}