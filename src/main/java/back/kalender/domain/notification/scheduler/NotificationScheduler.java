package back.kalender.domain.notification.scheduler;

import back.kalender.domain.notification.enums.NotificationType;
import back.kalender.domain.notification.service.NotificationService;
import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.entity.PartyMember;
import back.kalender.domain.party.repository.PartyMemberRepository;
import back.kalender.domain.party.repository.PartyRepository;
import back.kalender.domain.schedule.entity.Schedule;
import back.kalender.domain.schedule.enums.ScheduleCategory;
import back.kalender.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final ScheduleRepository scheduleRepository;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void sendScheduledNotifications() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        List<Schedule> todaySchedules = scheduleRepository.findAllByScheduleTimeBetween(startOfDay, endOfDay);

        if (todaySchedules.isEmpty()) {
            log.info("오늘은 예정된 일정이 없습니다.");
            return;
        }

        int count = 0;
        for (Schedule schedule : todaySchedules) {
            List<Party> parties = partyRepository.findAllByScheduleId(schedule.getId());

            for (Party party : parties) {
                List<PartyMember> activeMembers = partyMemberRepository.findActiveMembers(party.getId());

                for (PartyMember member : activeMembers) {
                    sendNotification(member.getUserId(), schedule, party);
                    count++;
                }
            }
        }
    }

    private void sendNotification(Long userId, Schedule schedule, Party party) {
        String title = "오늘의 일정 알림";
        String content;
        String url = "/party/" + party.getId();

        if (schedule.getScheduleCategory() == ScheduleCategory.BIRTHDAY) {
            content = String.format("오늘은 [%s]입니다. 다함께 축하해주세요! 🎂", schedule.getTitle());
        } else {
            String timeStr = schedule.getScheduleTime().format(DateTimeFormatter.ofPattern("HH시 mm분"));
            content = String.format("오늘 %s에 [%s] 일정이 있습니다!", timeStr, schedule.getTitle());
        }

        notificationService.send(
                userId,
                NotificationType.EVENT_REMINDER,
                title,
                content,
                url
        );
    }
}