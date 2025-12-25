package back.kalender.global.initData.notification;

import back.kalender.domain.notification.entity.Notification;
import back.kalender.domain.notification.enums.NotificationType;
import back.kalender.domain.notification.repository.NotificationRepository;
import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.repository.PartyRepository;
import back.kalender.domain.schedule.entity.Schedule;
import back.kalender.domain.schedule.repository.ScheduleRepository;
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;


@Component
@Profile({"prod", "dev"})
@Order(9) // Party(6), PartyApplication(7), Chat(8) 이후 실행
@RequiredArgsConstructor
@Slf4j
public class NotificationBaseInitData implements ApplicationRunner {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PartyRepository partyRepository;
    private final ScheduleRepository scheduleRepository;

    private final Random random = new Random();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (notificationRepository.count() > 0) {
            log.info("Notification base data already initialized");
            return;
        }
        createNotifications();
    }

    private void createNotifications() {
        List<User> users = userRepository.findAll();
        List<Party> parties = partyRepository.findAll();
        List<Schedule> schedules = scheduleRepository.findAll();

        if (users.isEmpty() || parties.isEmpty()) return;

        int count = 0;

        for (User user : users) {
            // 1. [EVENT_REMINDER] 일정 알림 (사용자당 1~2개)
            count += createEventNotifications(user, schedules);

            // 2. [APPLY] 내가 방장일 때 받은 신청 알림 (랜덤)
            count += createApproveNotifications(user, users, parties);

            // 3. [ACCEPT/REJECT] 내가 신청했을 때 받은 결과 알림 (랜덤)
            count += createResultNotifications(user, parties);

            // 4. [KICK] 강퇴 알림 (드물게)
            count += createKickNotifications(user, parties);
        }

        log.info("=".repeat(60));
        log.info("Notification base data insertion complete!");
        log.info("Total notifications created: {}", count);
        log.info("=".repeat(60));
    }

    // 1. 일정 알림 생성
    private int createEventNotifications(User user, List<Schedule> schedules) {
        int cnt = 0;
        int loop = 1 + random.nextInt(2);

        for (int i = 0; i < loop; i++) {
            Schedule schedule = schedules.get(random.nextInt(schedules.size()));

            String timeStr = schedule.getScheduleTime().toLocalTime().toString();
            String content = String.format("오늘 %s에 %s 일정이 있습니다!", timeStr, schedule.getTitle());

            saveNotification(user.getId(), NotificationType.EVENT_REMINDER, "오늘의 일정 알림", content);
            cnt++;
        }
        return cnt;
    }

    // 2. 파티 신청 알림 (방장 입장에서)
    private int createApproveNotifications(User user, List<User> allUsers, List<Party> parties) {
        List<Party> myParties = parties.stream()
                .filter(p -> p.getLeaderId().equals(user.getId()))
                .toList();
        if (myParties.isEmpty()) return 0;

        if (random.nextBoolean()) return 0;

        Party myParty = myParties.get(random.nextInt(myParties.size()));
        User applicant = allUsers.stream()
                .filter(u -> !u.getId().equals(user.getId()))
                .findAny()
                .orElse(allUsers.get(0));

        String content = String.format("%s(%d/%s)님이 '%s' 파티에 신청했습니다.",
                applicant.getNickname(), applicant.getAge(), applicant.getGender(), myParty.getPartyName());

        saveNotification(user.getId(), NotificationType.APPLY, "새로운 파티 신청", content);
        return 1;
    }

    // 3. 수락/거절 알림 (참여자 입장에서)
    private int createResultNotifications(User user, List<Party> parties) {
        int cnt = 0;
        Party party = parties.get(random.nextInt(parties.size()));

        if (random.nextBoolean()) {
            String content = String.format("'%s' 파티 신청이 수락되었습니다.", party.getPartyName());
            saveNotification(user.getId(), NotificationType.ACCEPT, "파티 수락 알림", content);
            cnt++;
        }

        if (random.nextInt(10) < 3) {
            String content = String.format("'%s' 파티 신청이 거절되었습니다.", party.getPartyName());
            saveNotification(user.getId(), NotificationType.REJECT, "파티 거절 알림", content);
            cnt++;
        }
        return cnt;
    }

    // 4. 강퇴 알림 (KICK)
    private int createKickNotifications(User user, List<Party> parties) {
        if (random.nextInt(10) > 0) return 0;

        Party party = parties.get(random.nextInt(parties.size()));

        String content = String.format("\"%s\" 파티에서 강퇴되었습니다. 참여자들을 평가해주세요.", party.getPartyName());

        saveNotification(user.getId(), NotificationType.KICK, "파티에서 강퇴되었습니다", content);
        return 1;
    }

    private void saveNotification(Long userId, NotificationType type, String title, String content) {
        Notification notification = new Notification(userId, type, title, content);

        if (random.nextBoolean()) {
            notification.markAsRead();
        }

        notificationRepository.save(notification);
    }
}