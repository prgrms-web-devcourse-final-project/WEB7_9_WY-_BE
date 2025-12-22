package back.kalender.domain.notification.response;

import back.kalender.domain.notification.entity.Notification;
import back.kalender.domain.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record NotificationResponse(
    @Schema(description = "알림 ID", example = "1")
    Long notificationId,

    @Schema(description = "알림 유형", example = "ACCEPT")
    NotificationType notificationType,

    @Schema(description = "알림 제목", example = "BTS 콘서트")
    String title,

    @Schema(description = "알림 내용", example = "오늘 8시에 BTS 콘서트 일정이 있습니다.")
    String content,

    @Schema(description = "클릭시 이동할 URL", example = "/events/1")
    String targetUrl,

    @Schema(description = "읽음 여부", example = "false")
    Boolean isRead,

    @Schema(description = "알림 생성 시간", example = "2024-06-15T14:30:00")
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
