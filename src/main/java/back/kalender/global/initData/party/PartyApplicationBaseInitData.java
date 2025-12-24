package back.kalender.global.initData.party;

import back.kalender.domain.chat.entity.ChatMessage;
import back.kalender.domain.chat.repository.ChatMessageRepository;
import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.entity.PartyApplication;
import back.kalender.domain.party.entity.PartyMember;
import back.kalender.domain.party.enums.ApplicationStatus;
import back.kalender.domain.party.enums.PartyStatus;
import back.kalender.domain.party.repository.PartyApplicationRepository;
import back.kalender.domain.party.repository.PartyMemberRepository;
import back.kalender.domain.party.repository.PartyRepository;
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

import java.util.*;

@Component
@Profile({"prod", "dev"})
@Order(7)
@RequiredArgsConstructor
@Slf4j
public class PartyApplicationBaseInitData implements ApplicationRunner {

    private final PartyApplicationRepository partyApplicationRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PartyRepository partyRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (partyApplicationRepository.count() > 0) {
            log.info("PartyApplication base data already initialized");
            return;
        }
        createApplicationsAndMembers();
    }

    private void createApplicationsAndMembers() {
        List<Party> parties = partyRepository.findAll();
        List<User> users = userRepository.findAll();

        if (parties.isEmpty() || users.size() < 2) {
            log.warn("Not enough parties or users to create applications");
            return;
        }

        int totalApplications = 0;

        for (Party party : parties) {
            PartyStatus partyStatus = party.getStatus();

            // 파티 상태별로 다른 로직 적용
            totalApplications += switch (partyStatus) {
                case RECRUITING -> handleRecruitingParty(party, users);
                case CLOSED -> handleClosedParty(party, users);
                case COMPLETED -> handleCompletedParty(party, users);
                case CANCELLED -> handleCancelledParty(party, users);
            };
        }

        // 통계 계산
        long approvedCount = partyApplicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.APPROVED).count();
        long rejectedCount = partyApplicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.REJECTED).count();
        long pendingCount = partyApplicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.PENDING).count();

        log.info("=".repeat(60));
        log.info("PartyApplication base data initialized:");
        log.info("  Total Applications: {}", totalApplications);
        log.info("  APPROVED: {} (with PartyMember + JOIN message)", approvedCount);
        log.info("  REJECTED: {}", rejectedCount);
        log.info("  PENDING: {}", pendingCount);
        log.info("=".repeat(60));
    }

    /**
     * RECRUITING 파티: 신규 신청 생성 (PENDING, APPROVED, REJECTED)
     */
    private int handleRecruitingParty(Party party, List<User> users) {
        int applicationCount = 0;

        // 3~6명의 신청자
        int targetApplications = 3 + (int)(Math.random() * 4);

        Set<Long> appliedUserIds = new HashSet<>();
        appliedUserIds.add(party.getLeaderId());

        for (int i = 0; i < targetApplications && appliedUserIds.size() < users.size(); i++) {
            User applicant = getRandomUser(users, appliedUserIds);
            if (applicant == null) break;

            appliedUserIds.add(applicant.getId());

            // 신청 생성
            PartyApplication application = PartyApplication.create(
                    party.getId(),
                    applicant.getId(),
                    party.getLeaderId()
            );

            // 확률적 처리
            double random = Math.random();

            if (random < 0.3 && !party.isFull()) {
                // 30% APPROVED
                application.approve();

                // PartyMember 생성
                partyMemberRepository.save(
                        PartyMember.createMember(party.getId(), applicant.getId())
                );

                // JOIN 메시지 생성
                chatMessageRepository.save(
                        ChatMessage.createJoinMessage(party.getId(), applicant.getId())
                );

                // currentMembers 증가
                party.incrementCurrentMembers();
                partyRepository.save(party);

            } else if (random < 0.5) {
                // 20% REJECTED
                application.reject();
            }
            // 나머지 50%는 PENDING

            partyApplicationRepository.save(application);
            applicationCount++;
        }

        return applicationCount;
    }

    /**
     * CLOSED 파티: 정원이 꽉 차도록 APPROVED 신청 생성
     */
    private int handleClosedParty(Party party, List<User> users) {
        int applicationCount = 0;

        Set<Long> appliedUserIds = new HashSet<>();
        appliedUserIds.add(party.getLeaderId());

        // 정원이 꽉 찰 때까지 APPROVED 신청 생성
        int neededMembers = party.getMaxMembers() - party.getCurrentMembers();

        for (int i = 0; i < neededMembers && appliedUserIds.size() < users.size(); i++) {
            User applicant = getRandomUser(users, appliedUserIds);
            if (applicant == null) break;

            appliedUserIds.add(applicant.getId());

            // APPROVED 신청 생성
            PartyApplication application = PartyApplication.create(
                    party.getId(),
                    applicant.getId(),
                    party.getLeaderId()
            );
            application.approve();
            partyApplicationRepository.save(application);

            // PartyMember 생성
            partyMemberRepository.save(
                    PartyMember.createMember(party.getId(), applicant.getId())
            );

            // JOIN 메시지 생성
            chatMessageRepository.save(
                    ChatMessage.createJoinMessage(party.getId(), applicant.getId())
            );

            // currentMembers 증가
            party.incrementCurrentMembers();

            applicationCount++;
        }

        partyRepository.save(party);

        // 추가로 2~3명의 REJECTED 신청 생성 (정원 마감으로 거절됨)
        int rejectedCount = 2 + (int)(Math.random() * 2);
        for (int i = 0; i < rejectedCount && appliedUserIds.size() < users.size(); i++) {
            User applicant = getRandomUser(users, appliedUserIds);
            if (applicant == null) break;

            appliedUserIds.add(applicant.getId());

            PartyApplication application = PartyApplication.create(
                    party.getId(),
                    applicant.getId(),
                    party.getLeaderId()
            );
            application.reject();
            partyApplicationRepository.save(application);
            applicationCount++;
        }

        return applicationCount;
    }

    /**
     * COMPLETED 파티: 과거 신청들 생성
     */
    private int handleCompletedParty(Party party, List<User> users) {
        int applicationCount = 0;

        // 2~4명의 과거 멤버
        int memberCount = 2 + (int)(Math.random() * 3);
        Set<Long> appliedUserIds = new HashSet<>();
        appliedUserIds.add(party.getLeaderId());

        for (int i = 0; i < memberCount && appliedUserIds.size() < users.size(); i++) {
            User applicant = getRandomUser(users, appliedUserIds);
            if (applicant == null) break;

            appliedUserIds.add(applicant.getId());

            PartyApplication application = PartyApplication.create(
                    party.getId(),
                    applicant.getId(),
                    party.getLeaderId()
            );

            // 80% APPROVED, 20% REJECTED
            if (Math.random() < 0.8) {
                application.approve();

                partyMemberRepository.save(
                        PartyMember.createMember(party.getId(), applicant.getId())
                );

                chatMessageRepository.save(
                        ChatMessage.createJoinMessage(party.getId(), applicant.getId())
                );

                party.incrementCurrentMembers();
            } else {
                application.reject();
            }

            partyApplicationRepository.save(application);
            applicationCount++;
        }

        partyRepository.save(party);
        return applicationCount;
    }

    /**
     * CANCELLED 파티: 소수의 신청만 생성
     */
    private int handleCancelledParty(Party party, List<User> users) {
        int applicationCount = 0;

        // 1~2명의 신청만
        int targetApplications = 1 + (int)(Math.random() * 2);
        Set<Long> appliedUserIds = new HashSet<>();
        appliedUserIds.add(party.getLeaderId());

        for (int i = 0; i < targetApplications && appliedUserIds.size() < users.size(); i++) {
            User applicant = getRandomUser(users, appliedUserIds);
            if (applicant == null) break;

            appliedUserIds.add(applicant.getId());

            // PENDING 상태로만 생성 (파티가 취소되어 처리 안 됨)
            PartyApplication application = PartyApplication.create(
                    party.getId(),
                    applicant.getId(),
                    party.getLeaderId()
            );
            partyApplicationRepository.save(application);
            applicationCount++;
        }

        return applicationCount;
    }

    private User getRandomUser(List<User> users, Set<Long> excludeIds) {
        List<User> availableUsers = users.stream()
                .filter(u -> !excludeIds.contains(u.getId()))
                .toList();

        if (availableUsers.isEmpty()) {
            return null;
        }

        return availableUsers.get((int)(Math.random() * availableUsers.size()));
    }
}