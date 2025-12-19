package back.kalender.domain.notification.entity;

import back.kalender.domain.notification.enums.NotificationType;
import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "content", length = 255)
    private String content;

    @Column(name = "target_url", length = 255)
    private String targetUrl;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    public Notification(Long userId, NotificationType notificationType, String title, String content, String targetUrl) {
        this.userId = userId;
        this.notificationType = notificationType;
        this.title = title;
        this.content = content;
        this.targetUrl = targetUrl;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}