package back.kalender.global.initData.notification;

import back.kalender.domain.notification.entity.Notification;
import back.kalender.domain.notification.enums.NotificationType;
import back.kalender.domain.notification.repository.NotificationRepository;
import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.entity.PartyApplication;
import back.kalender.domain.party.entity.PartyMember;
import back.kalender.domain.party.enums.ApplicationStatus;
import back.kalender.domain.party.repository.PartyApplicationRepository;
import back.kalender.domain.party.repository.PartyMemberRepository;
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
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Component
@Profile({"prod", "dev"})
@Order(9)
@RequiredArgsConstructor
@Slf4j
public class NotificationBaseInitData implements ApplicationRunner {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PartyRepository partyRepository;
    private final ScheduleRepository scheduleRepository;
    private final PartyApplicationRepository partyApplicationRepository;
    private final PartyMemberRepository partyMemberRepository;

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

        Map<Long, Party> partyMap = parties.stream()
                .collect(Collectors.toMap(Party::getId, p -> p));

        int count = 0;

        for (User user : users) {
            // 1. [EVENT_REMINDER] 일정 알림
            count += createEventNotifications(user, schedules);

            // 2. [APPLY] 내가 방장일 때 받은 신청 알림 (실제 신청자 기반)
            count += createApproveNotifications(user, users, parties);

            // 3. [ACCEPT/REJECT] 결과 알림 (실제 내 신청 내역 기반)
            count += createResultNotifications(user, partyMap);

            // 4. [KICK] 강퇴 알림 (실제 내가 참여 중인 파티 기반)
            count += createKickNotifications(user, partyMap);
        }

        log.info("=".repeat(60));
        log.info("Notification base data insertion complete!");
        log.info("Total notifications created: {}", count);
        log.info("=".repeat(60));
    }

    // 1. 일정 알림
    private int createEventNotifications(User user, List<Schedule> schedules) {
        int cnt = 0;
        int loop = 1 + random.nextInt(2);

        for (int i = 0; i < loop; i++) {
            Schedule schedule = schedules.get(random.nextInt(schedules.size()));

            String timeStr = schedule.getScheduleTime().toLocalTime().toString();
            String content = String.format("오늘 %s에 %s 일정이 있습니다!", timeStr, schedule.getTitle());

            saveNotification(user.getId(), NotificationType.EVENT_REMINDER, "일정 알림", content);
            cnt++;
        }
        return cnt;
    }

    // 2. 파티 신청 알림
    private int createApproveNotifications(User user, List<User> allUsers, List<Party> parties) {
        List<Party> myParties = parties.stream()
                .filter(p -> p.getLeaderId().equals(user.getId()))
                .toList();

        if (myParties.isEmpty()) return 0;
        if (random.nextBoolean()) return 0;

        Party myParty = myParties.get(random.nextInt(myParties.size()));
        List<PartyApplication> applications = partyApplicationRepository.findByPartyId(myParty.getId());

        if (applications.isEmpty()) return 0;

        PartyApplication realApplication = applications.get(random.nextInt(applications.size()));
        Long applicantId = realApplication.getApplicantId();

        User applicant = allUsers.stream()
                .filter(u -> u.getId().equals(applicantId))
                .findFirst()
                .orElse(null);

        if (applicant == null) return 0;

        String content = String.format("%s(%d/%s)님이 '%s' 파티를 신청했습니다.",
                applicant.getNickname(), applicant.getAge(), applicant.getGender(), myParty.getPartyName());

        saveNotification(user.getId(), NotificationType.APPLY, "새로운 파티 신청", content);
        return 1;
    }

    // 3. 수락/거절 알림
    private int createResultNotifications(User user, Map<Long, Party> partyMap) {
        List<PartyApplication> myApplications = partyApplicationRepository.findByApplicantId(user.getId());

        if (myApplications.isEmpty()) return 0;

        int cnt = 0;

        for (PartyApplication app : myApplications) {
            Party party = partyMap.get(app.getPartyId());
            if (party == null) continue;

            if (random.nextBoolean()) continue;

            if (app.getStatus() == ApplicationStatus.APPROVED) {
                String content = String.format("'%s' 파티 신청이 승인되었습니다.", party.getPartyName());
                saveNotification(user.getId(), NotificationType.ACCEPT, "파티 수락 알림", content);
                cnt++;
            }
            else if (app.getStatus() == ApplicationStatus.REJECTED) {
                String content = String.format("'%s' 파티 신청이 거절되었습니다.", party.getPartyName());
                saveNotification(user.getId(), NotificationType.REJECT, "파티 거절 알림", content);
                cnt++;
            }
        }
        return cnt;
    }

    // 4. 강퇴 알림
    private int createKickNotifications(User user, Map<Long, Party> partyMap) {
        if (random.nextInt(10) > 1) return 0;

        List<PartyMember> myMemberships = partyMemberRepository.findByUserId(user.getId());

        List<PartyMember> memberOnly = myMemberships.stream()
                .filter(m -> {
                    Party p = partyMap.get(m.getPartyId());
                    return p != null && !p.getLeaderId().equals(user.getId());
                })
                .toList();

        if (memberOnly.isEmpty()) return 0;

        // 강퇴 알림 -> 실제 DB에선 삭제되진 않음, 알림 테스트용으로만
        //TODO: 추후에 실제로 강퇴처리 시킬 때 status=KICKED 처리하는 로직으로 수정할 것
        PartyMember targetMembership = memberOnly.get(random.nextInt(memberOnly.size()));
        Party party = partyMap.get(targetMembership.getPartyId());

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