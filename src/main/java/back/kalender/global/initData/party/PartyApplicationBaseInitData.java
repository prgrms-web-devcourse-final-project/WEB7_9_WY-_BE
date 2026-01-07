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
@Order(8)
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
        int totalMembers = 0;
        int overCapacityPrevented = 0; // 초과 방지 카운트

        for (Party party : parties) {
            PartyStatus partyStatus = party.getStatus();

            ApplicationResult result = switch (partyStatus) {
                case RECRUITING -> handleRecruitingParty(party, users);
                case CLOSED -> handleClosedParty(party, users);
                case COMPLETED -> handleCompletedParty(party, users);
                case CANCELLED -> handleCancelledParty(party, users);
            };

            totalApplications += result.applicationCount;
            totalMembers += result.memberCount;

            // 정원 초과 검증
            if (party.getCurrentMembers() > party.getMaxMembers()) {
                log.error("❌ Party {} has {} members but max is {}",
                        party.getId(), party.getCurrentMembers(), party.getMaxMembers());
                overCapacityPrevented++;
            }
        }

        // 통계 출력
        long approvedCount = partyApplicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.APPROVED).count();
        long rejectedCount = partyApplicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.REJECTED).count();
        long pendingCount = partyApplicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.PENDING).count();

        log.info("=".repeat(60));
        log.info("PartyApplication base data initialized:");
        log.info("  Total Applications: {}", totalApplications);
        log.info("    - APPROVED: {} (with PartyMember + JOIN message)", approvedCount);
        log.info("    - REJECTED: {}", rejectedCount);
        log.info("    - PENDING: {}", pendingCount);
        log.info("  Total Active Members Added: {}", totalMembers);
        log.info("  Over-capacity Issues: {}", overCapacityPrevented);
        log.info("Note: All members are active (leftAt and kickedAt are null)");
        log.info("Note: Leave/Kick functionality will be tested manually");
        log.info("=".repeat(60));
    }

    /**
     * RECRUITING 파티: 다양한 신청 상태 생성
     * - 30% APPROVED (정원 미달 시만)
     * - 20% REJECTED
     * - 50% PENDING
     */
    private ApplicationResult handleRecruitingParty(Party party, List<User> users) {
        int applicationCount = 0;
        int memberCount = 0;

        int targetApplications = 3 + (int)(Math.random() * 4); // 3~6개 신청

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

            // 30% 확률로 승인 (단, 정원이 남아있을 때만)
            if (random < 0.3 && !party.isFull()) {
                application.approve();
                partyApplicationRepository.save(application);

                PartyMember member = PartyMember.createMember(party.getId(), applicant.getId());
                partyMemberRepository.save(member);

                chatMessageRepository.save(
                        ChatMessage.createJoinMessage(party.getId(), applicant.getId())
                );

                party.incrementCurrentMembers();
                memberCount++;
            }
            // 20% 확률로 거절
            else if (random < 0.5) {
                application.reject();
                partyApplicationRepository.save(application);
            }
            // 나머지 50%는 PENDING (기본값)
            else {
                partyApplicationRepository.save(application);
            }

            applicationCount++;
        }

        partyRepository.save(party);
        return new ApplicationResult(applicationCount, memberCount);
    }

    /**
     * CLOSED 파티: 정원을 정확히 채움
     * - 정원만큼 APPROVED
     * - 추가로 2~3개 REJECTED
     */
    private ApplicationResult handleClosedParty(Party party, List<User> users) {
        int applicationCount = 0;
        int memberCount = 0;

        Set<Long> appliedUserIds = new HashSet<>();
        appliedUserIds.add(party.getLeaderId());

        // 정원을 채우기 위해 필요한 멤버 수 (리더 제외)
        // currentMembers는 이미 1 (리더 포함)
        int neededMembers = party.getMaxMembers() - party.getCurrentMembers();

        // 정원만큼 승인된 신청 생성
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

            PartyMember member = PartyMember.createMember(party.getId(), applicant.getId());
            partyMemberRepository.save(member);

            chatMessageRepository.save(
                    ChatMessage.createJoinMessage(party.getId(), applicant.getId())
            );

            party.incrementCurrentMembers();
            applicationCount++;
            memberCount++;
        }

        // 추가로 거절된 신청 생성 (2~3개)
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

        partyRepository.save(party);
        return new ApplicationResult(applicationCount, memberCount);
    }

    /**
     * COMPLETED 파티: 멤버가 있는 상태
     * - maxMembers를 초과하지 않는 범위에서 멤버 추가
     * - 모든 멤버는 활성 상태
     */
    private ApplicationResult handleCompletedParty(Party party, List<User> users) {
        int applicationCount = 0;
        int memberCount = 0;

        // 추가 가능한 최대 멤버 수 계산 (리더는 이미 포함됨)
        int maxAdditionalMembers = party.getMaxMembers() - party.getCurrentMembers();

        // COMPLETED 파티는 2~4명 사이로 설정하되, maxMembers를 초과하지 않도록
        int desiredMembers = 2 + (int)(Math.random() * 3); // 2~4명
        int memberTarget = Math.min(desiredMembers, maxAdditionalMembers);

        Set<Long> appliedUserIds = new HashSet<>();
        appliedUserIds.add(party.getLeaderId());

        for (int i = 0; i < memberTarget && appliedUserIds.size() < users.size(); i++) {
            User applicant = getRandomUser(users, appliedUserIds);
            if (applicant == null) break;

            appliedUserIds.add(applicant.getId());

            PartyApplication application = PartyApplication.create(
                    party.getId(),
                    applicant.getId(),
                    party.getLeaderId()
            );

            // 80% 확률로 승인
            if (Math.random() < 0.8) {
                application.approve();
                partyApplicationRepository.save(application);

                PartyMember member = PartyMember.createMember(party.getId(), applicant.getId());
                partyMemberRepository.save(member);

                chatMessageRepository.save(
                        ChatMessage.createJoinMessage(party.getId(), applicant.getId())
                );

                party.incrementCurrentMembers();
                memberCount++;
            } else {
                application.reject();
                partyApplicationRepository.save(application);
            }

            applicationCount++;
        }

        partyRepository.save(party);
        return new ApplicationResult(applicationCount, memberCount);
    }

    /**
     * CANCELLED 파티: 소수의 멤버와 PENDING 신청
     * - maxMembers를 초과하지 않는 범위에서 멤버 추가
     * - 모든 멤버는 활성 상태
     */
    private ApplicationResult handleCancelledParty(Party party, List<User> users) {
        int applicationCount = 0;
        int memberCount = 0;

        Set<Long> appliedUserIds = new HashSet<>();
        appliedUserIds.add(party.getLeaderId());

        // 추가 가능한 최대 멤버 수 계산
        int maxAdditionalMembers = party.getMaxMembers() - party.getCurrentMembers();

        // CANCELLED 파티는 1~3명 사이로 설정하되, maxMembers를 초과하지 않도록
        int desiredMembers = 1 + (int)(Math.random() * 3); // 1~3명
        int memberTarget = Math.min(desiredMembers, maxAdditionalMembers);

        for (int i = 0; i < memberTarget && appliedUserIds.size() < users.size(); i++) {
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

            PartyMember member = PartyMember.createMember(party.getId(), applicant.getId());
            partyMemberRepository.save(member);

            chatMessageRepository.save(
                    ChatMessage.createJoinMessage(party.getId(), applicant.getId())
            );

            party.incrementCurrentMembers();
            applicationCount++;
            memberCount++;
        }

        // 추가로 PENDING 신청 1~2개 생성
        int pendingCount = 1 + (int)(Math.random() * 2);
        for (int i = 0; i < pendingCount && appliedUserIds.size() < users.size(); i++) {
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

        partyRepository.save(party);
        return new ApplicationResult(applicationCount, memberCount);
    }

    /**
     * 랜덤 유저 선택 (이미 신청한 유저 제외)
     */
    private User getRandomUser(List<User> users, Set<Long> excludeIds) {
        List<User> availableUsers = users.stream()
                .filter(u -> !excludeIds.contains(u.getId()))
                .toList();

        if (availableUsers.isEmpty()) {
            return null;
        }

        return availableUsers.get((int)(Math.random() * availableUsers.size()));
    }

    /**
     * 결과 데이터를 담는 record
     */
    private record ApplicationResult(
            int applicationCount,
            int memberCount
    ) {}
}