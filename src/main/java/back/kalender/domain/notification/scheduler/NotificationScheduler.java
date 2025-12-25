package back.kalender.domain.notification.scheduler;

import back.kalender.domain.notification.enums.NotificationType;
import back.kalender.domain.notification.service.NotificationService;
import back.kalender.domain.party.dto.query.NotificationTarget;
import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.entity.PartyMember;
import back.kalender.domain.party.repository.PartyMemberRepository;
import back.kalender.domain.party.repository.PartyRepository;
import back.kalender.domain.schedule.entity.Schedule;
import back.kalender.domain.schedule.enums.ScheduleCategory;
import back.kalender.domain.schedule.repository.ScheduleAlarmRepository;
import back.kalender.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationService notificationService;
    private final ScheduleAlarmRepository scheduleAlarmRepository;
    @Scheduled(cron = "0 0 0 * * *")
    public void sendScheduledNotifications() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        List<NotificationTarget> targets = scheduleAlarmRepository.findScheduleNotificationTargets(startOfDay, endOfDay);

        if (targets.isEmpty()) {
            log.info("오늘은 예정된 일정이 없습니다.");
            return;
        }

        int count = 0;
        for (NotificationTarget target : targets) {
            try {
                sendNotification(target);
                count++;
            } catch (Exception e) {
                log.error("스케줄러 알림 발송 실패 (UserId: {}, PartyId: {}): {}",
                        target.userId(), target.scheduleTitle(), e.getMessage());
            }
        }

        log.info("[스케줄러 종료] 총 {}건의 알림 발송 완료", count);
    }

    private void sendNotification(NotificationTarget target) {
        String title = "오늘의 일정 알림";
        String content;
        String url = "/schedule/" + target.scheduleTitle();

        if (target.category() == ScheduleCategory.BIRTHDAY || target.category() == ScheduleCategory.ANNIVERSARY) {
            content = String.format("오늘은 %s입니다. 다함께 축하해주세요! 🎂", target.scheduleTitle());
        } else {
            String timeStr = target.scheduleTime().format(DateTimeFormatter.ofPattern("HH시 mm분"));
            content = String.format("오늘 %s에 %s 일정이 있습니다!", timeStr, target.scheduleTitle());
        }

        notificationService.send(
                target.userId(),
                NotificationType.EVENT_REMINDER,
                title,
                content
        );
    }
}