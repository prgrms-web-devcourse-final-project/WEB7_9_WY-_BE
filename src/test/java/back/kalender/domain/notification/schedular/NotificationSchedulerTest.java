package back.kalender.domain.notification.schedular;

import back.kalender.domain.notification.enums.NotificationType;
import back.kalender.domain.notification.scheduler.NotificationScheduler;
import back.kalender.domain.notification.service.NotificationService;
import back.kalender.domain.party.dto.query.NotificationTarget;
import back.kalender.domain.schedule.enums.ScheduleCategory;
import back.kalender.domain.schedule.repository.ScheduleAlarmRepository;
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
    private ScheduleAlarmRepository scheduleAlarmRepository;

    @Test
    @DisplayName("알림 받기를 신청한 유저에게는 정상적으로 일정 알림이 전송된다")
    void sendScheduledNotifications_Success() {
        LocalDateTime fixedTime = LocalDateTime.of(2025, 12, 25, 18, 0);

        NotificationTarget subscribedTarget = new NotificationTarget(
                1L, null, "BTS 콘서트", ScheduleCategory.CONCERT, fixedTime
        );

        given(scheduleAlarmRepository.findNotificationTargets(any(), any()))
                .willReturn(List.of(subscribedTarget));

        notificationScheduler.sendScheduledNotifications();

        verify(notificationService).send(
                eq(1L),
                eq(NotificationType.EVENT_REMINDER),
                anyString(),
                eq("오늘 18시 00분에 BTS 콘서트 일정이 있습니다!"),
                contains("/schedule/BTS 콘서트")
        );
    }

    @Test
    @DisplayName("파티에 참여했더라도 '알림 받기'를 신청하지 않았다면 알림이 오지 않아야 한다")
    void sendScheduledNotifications_NoAlarm_IfNotSubscribed() {
        given(scheduleAlarmRepository.findNotificationTargets(any(), any()))
                .willReturn(List.of());

        notificationScheduler.sendScheduledNotifications();

        verify(notificationService, times(0)).send(any(), any(), any(), any(), any());
    }
}