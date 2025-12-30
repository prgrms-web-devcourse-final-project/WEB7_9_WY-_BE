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

            totalApplications += switch (partyStatus) {
                case RECRUITING -> handleRecruitingParty(party, users);
                case CLOSED -> handleClosedParty(party, users);
                case COMPLETED -> handleCompletedParty(party, users);
                case CANCELLED -> handleCancelledParty(party, users);
            };
        }

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

    private int handleRecruitingParty(Party party, List<User> users) {
        int applicationCount = 0;
        int targetApplications = 3 + (int)(Math.random() * 4);

        Set<Long> appliedUserIds = new HashSet<>();
        appliedUserIds.add(party.getLeaderId());

        for (int i = 0; i < targetApplications && appliedUserIds.size() < users.size(); i++) {
            User applicant = getRandomUser(users, appliedUserIds);
            if (applicant == null) break;

            appliedUserIds.add(applicant.getId());

            PartyApplication application = PartyApplication.create(
                    party.getId(),
                    applicant.getId(),
                    party.getLeaderId()
            );

            double random = Math.random();

            if (random < 0.3 && !party.isFull()) {
                application.approve();

                partyMemberRepository.save(
                        PartyMember.createMember(party.getId(), applicant.getId())
                );

                chatMessageRepository.save(
                        ChatMessage.createJoinMessage(party.getId(), applicant.getId())
                );

                party.incrementCurrentMembers();
                partyRepository.save(party);

            } else if (random < 0.5) {
                application.reject();
            }

            partyApplicationRepository.save(application);
            applicationCount++;
        }

        return applicationCount;
    }

    private int handleClosedParty(Party party, List<User> users) {
        int applicationCount = 0;

        Set<Long> appliedUserIds = new HashSet<>();
        appliedUserIds.add(party.getLeaderId());

        int neededMembers = party.getMaxMembers() - 1;

        for (int i = 0; i < neededMembers && appliedUserIds.size() < users.size(); i++) {
            User applicant = getRandomUser(users, appliedUserIds);
            if (applicant == null) break;

            appliedUserIds.add(applicant.getId());

            PartyApplication application = PartyApplication.create(
                    party.getId(),
                    applicant.getId(),
                    party.getLeaderId()
            );
            application.approve();
            partyApplicationRepository.save(application);

            partyMemberRepository.save(
                    PartyMember.createMember(party.getId(), applicant.getId())
            );

            chatMessageRepository.save(
                    ChatMessage.createJoinMessage(party.getId(), applicant.getId())
            );

            party.incrementCurrentMembers();

            applicationCount++;
        }

        partyRepository.save(party);

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

    private int handleCompletedParty(Party party, List<User> users) {
        int applicationCount = 0;

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

    private int handleCancelledParty(Party party, List<User> users) {
        int applicationCount = 0;

        int targetApplications = 1 + (int)(Math.random() * 2);
        Set<Long> appliedUserIds = new HashSet<>();
        appliedUserIds.add(party.getLeaderId());

        for (int i = 0; i < targetApplications && appliedUserIds.size() < users.size(); i++) {
            User applicant = getRandomUser(users, appliedUserIds);
            if (applicant == null) break;

            appliedUserIds.add(applicant.getId());

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