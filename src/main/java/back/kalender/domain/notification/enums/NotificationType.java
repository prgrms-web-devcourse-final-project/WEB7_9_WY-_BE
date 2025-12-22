package back.kalender.domain.notification.enums;

public enum NotificationType {
    EVENT_REMINDER, // 일정 알림
    APPLY, // 방장에게 신청 알림
    ACCEPT, // 신청자에게 신청 수락 알림
    REJECT, // 신청자에게 신청 거절 알림
    KICK, // 참여자에게 강퇴 알림
    SYSTEM_ALERT // 시스템상의 중요 알림
}
