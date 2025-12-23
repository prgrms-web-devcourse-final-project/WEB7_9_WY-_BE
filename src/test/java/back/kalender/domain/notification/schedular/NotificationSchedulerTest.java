package back.kalender.domain.notification.scheduler;

import back.kalender.domain.notification.enums.NotificationType;
import back.kalender.domain.notification.service.NotificationService;
import back.kalender.domain.party.dto.query.NotificationTarget;
import back.kalender.domain.party.repository.PartyRepository;
import back.kalender.domain.schedule.enums.ScheduleCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @InjectMocks
    private NotificationScheduler notificationScheduler;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PartyRepository partyRepository;

    @Test
    @DisplayName("카테고리별(생일, 기념일, 일반)로 알림 메시지가 다르게 발송되어야 한다")
    void sendScheduledNotifications_ShouldSendDifferentMessagesByCategory() {
        LocalDateTime fixedTime = LocalDateTime.of(2024, 12, 25, 0, 0);

        NotificationTarget birthdayTarget = new NotificationTarget(
                1L, 100L, "지민 생일", ScheduleCategory.BIRTHDAY, fixedTime
        );

        NotificationTarget anniversaryTarget = new NotificationTarget(
                2L, 200L, "데뷔 10주년", ScheduleCategory.ANNIVERSARY, fixedTime
        );

        NotificationTarget concertTarget = new NotificationTarget(
                3L, 300L, "흠뻑쇼", ScheduleCategory.CONCERT, fixedTime
        );

        given(partyRepository.findNotificationTargets(any(), any()))
                .willReturn(List.of(birthdayTarget, anniversaryTarget, concertTarget));

        notificationScheduler.sendScheduledNotifications();

        verify(notificationService).send(
                eq(1L),
                eq(NotificationType.EVENT_REMINDER),
                anyString(),
                eq("오늘은 지민 생일입니다. 다함께 축하해주세요! 🎂"),
                contains("/party/100")
        );

        verify(notificationService).send(
                eq(2L),
                eq(NotificationType.EVENT_REMINDER),
                anyString(),
                eq("오늘은 데뷔 10주년입니다. 다함께 축하해주세요! 🎂"),
                contains("/party/200")
        );

        verify(notificationService).send(
                eq(3L),
                eq(NotificationType.EVENT_REMINDER),
                anyString(),
                eq("오늘 00시 00분에 흠뻑쇼 일정이 있습니다!"),
                contains("/party/300")
        );

        verify(notificationService, times(3)).send(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("알림 대상이 없으면 서비스 호출 없이 종료되어야 한다")
    void sendScheduledNotifications_WhenNoTargets_ShouldNotCallService() {
        given(partyRepository.findNotificationTargets(any(), any()))
                .willReturn(List.of());

        notificationScheduler.sendScheduledNotifications();

        verify(notificationService, times(0)).send(any(), any(), any(), any(), any());
    }
}