package back.kalender.domain.notification.response;

import back.kalender.domain.notification.entity.Notification;
import back.kalender.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    NotificationType notificationType,
    String title,
    String content,
    String targetUrl,
    Boolean isRead,
    LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetUrl(),
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }
}
