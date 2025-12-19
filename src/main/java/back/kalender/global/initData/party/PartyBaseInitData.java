//package back.kalender.global.initData.party;
//
//import back.kalender.domain.party.entity.*;
//import back.kalender.domain.party.repository.PartyApplicationRepository;
//import back.kalender.domain.party.repository.PartyMemberRepository;
//import back.kalender.domain.party.repository.PartyRepository;
//import back.kalender.domain.schedule.entity.Schedule;
//import back.kalender.domain.schedule.enums.ScheduleCategory;
//import back.kalender.domain.schedule.repository.ScheduleRepository;
//import back.kalender.domain.user.entity.User;
//import back.kalender.domain.user.repository.UserRepository;
//import back.kalender.global.common.Enum.Gender;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Profile;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//
//@Slf4j
//@Component
//@Profile({"local", "dev"})
//@RequiredArgsConstructor
//public class PartyBaseInitData {
//
//    private final UserRepository userRepository;
//    private final ScheduleRepository scheduleRepository;
//    private final PartyRepository partyRepository;
//    private final PartyMemberRepository partyMemberRepository;
//    private final PartyApplicationRepository partyApplicationRepository;
//    private final PasswordEncoder passwordEncoder; // 추가!
//
//    @PostConstruct
//    public void init() {
//        if (userRepository.findByEmail("leader1@test.com").isPresent()) {
//            log.info("===== Party 테스트 데이터가 이미 존재합니다. 초기화를 건너뜁니다. =====");
//            return;
//        }
//
//        initData();
//    }
//
//    @Transactional
//    public void initData() {
//        log.info("===== Party 테스트 데이터 초기화 시작 =====");
//
//        // 1. User 데이터 생성
//        User leader1 = createUser("leader1@test.com", "리더유저1", Gender.MALE, LocalDate.of(1995, 3, 15));
//        User leader2 = createUser("leader2@test.com", "리더유저2", Gender.FEMALE, LocalDate.of(1998, 7, 22));
//        User member1 = createUser("member1@test.com", "멤버유저1", Gender.MALE, LocalDate.of(2000, 1, 10));
//        User member2 = createUser("member2@test.com", "멤버유저2", Gender.FEMALE, LocalDate.of(2002, 5, 20));
//        User applicant1 = createUser("applicant1@test.com", "신청자1", Gender.MALE, LocalDate.of(1999, 11, 5));
//        User applicant2 = createUser("applicant2@test.com", "신청자2", Gender.FEMALE, LocalDate.of(2001, 8, 30));
//
//        log.info("User 데이터 생성 완료: 총 6명");
//        log.info("  ✅ 테스트 계정 비밀번호: password123!");
//
//        // 2. Schedule 데이터 생성
//        Schedule concert1 = createSchedule(
//                1L,
//                "BTS 콘서트 2025",
//                ScheduleCategory.CONCERT,
//                LocalDateTime.of(2025, 12, 31, 19, 0),
//                "잠실종합운동장"
//        );
//
//        Schedule concert2 = createSchedule(
//                2L,
//                "블랙핑크 월드투어",
//                ScheduleCategory.CONCERT,
//                LocalDateTime.of(2025, 11, 15, 20, 0),
//                "고척스카이돔"
//        );
//
//        Schedule fanmeeting = createSchedule(
//                1L,
//                "세븐틴 팬미팅",
//                ScheduleCategory.FAN_MEETING,
//                LocalDateTime.of(2025, 10, 20, 18, 0),
//                "올림픽공원 올림픽홀"
//        );
//
//        log.info("Schedule 데이터 생성 완료: 총 3개");
//
//        // 3. Party 데이터 생성
//
//        // 3-1. 모집중인 파티 (리더1)
//        Party party1 = createParty(
//                concert1.getId(),
//                leader1.getId(),
//                PartyType.LEAVE,
//                "즐거운 BTS 콘서트 출발팟",
//                "같이 즐겁게 가요!",
//                "강남역 3번출구",
//                "잠실종합운동장",
//                TransportType.SUBWAY,
//                4,
//                Gender.ANY,
//                PreferredAge.TWENTY
//        );
//
//        createPartyMember(party1.getId(), leader1.getId(), MemberRole.LEADER);
//        createPartyMember(party1.getId(), member1.getId(), MemberRole.MEMBER);
//        party1.incrementCurrentMembers();
//
//        createPartyApplication(party1.getId(), applicant1.getId(), leader1.getId(), ApplicationStatus.PENDING);
//
//        // 3-2. 모집 마감된 파티 (리더1)
//        Party party2 = createParty(
//                concert1.getId(),
//                leader1.getId(),
//                PartyType.ARRIVE,
//                "BTS 콘서트 복귀팟",
//                "공연 끝나고 같이 돌아가요",
//                "잠실종합운동장",
//                "강남역",
//                TransportType.TAXI,
//                3,
//                Gender.FEMALE,
//                PreferredAge.TWENTY
//        );
//        party2.changeStatus(PartyStatus.CLOSED);
//
//        createPartyMember(party2.getId(), leader1.getId(), MemberRole.LEADER);
//        createPartyMember(party2.getId(), member1.getId(), MemberRole.MEMBER);
//        createPartyMember(party2.getId(), member2.getId(), MemberRole.MEMBER);
//        party2.incrementCurrentMembers();
//        party2.incrementCurrentMembers();
//
//        // 3-3. 블랙핑크 파티 (리더2)
//        Party party3 = createParty(
//                concert2.getId(),
//                leader2.getId(),
//                PartyType.LEAVE,
//                "블핑 콘서트 같이 가요",
//                "20대 여성분들 환영합니다",
//                "신림역",
//                "고척스카이돔",
//                TransportType.BUS,
//                5,
//                Gender.FEMALE,
//                PreferredAge.TWENTY
//        );
//
//        createPartyMember(party3.getId(), leader2.getId(), MemberRole.LEADER);
//        createPartyMember(party3.getId(), member2.getId(), MemberRole.MEMBER);
//        party3.incrementCurrentMembers();
//
//        createPartyApplication(party3.getId(), applicant2.getId(), leader2.getId(), ApplicationStatus.PENDING);
//        PartyApplication rejectedApp = createPartyApplication(party3.getId(), applicant1.getId(), leader2.getId(), ApplicationStatus.PENDING);
//        rejectedApp.reject();
//
//        // 3-4. 세븐틴 팬미팅 파티 (리더2)
//        Party party4 = createParty(
//                fanmeeting.getId(),
//                leader2.getId(),
//                PartyType.LEAVE,
//                "세븐틴 팬미팅 출발",
//                "같이 가실 분~",
//                "홍대입구역",
//                "올림픽공원",
//                TransportType.SUBWAY,
//                4,
//                Gender.ANY,
//                PreferredAge.ANY
//        );
//
//        createPartyMember(party4.getId(), leader2.getId(), MemberRole.LEADER);
//
//        log.info("Party 데이터 생성 완료: 총 4개");
//        log.info("PartyMember 데이터 생성 완료: 총 7개");
//        log.info("PartyApplication 데이터 생성 완료: 총 3개");
//
//        log.info("===== Party 테스트 데이터 초기화 완료 =====");
//    }
//
//    private User createUser(String email, String nickname, Gender gender, LocalDate birthDate) {
//        // 수정: 비밀번호를 암호화하여 저장
//        String encodedPassword = passwordEncoder.encode("password123!");
//
//        User user = User.builder()
//                .email(email)
//                .password(encodedPassword) // 암호화된 비밀번호
//                .nickname(nickname)
//                .profileImage("https://example.com/profile/" + email + ".jpg")
//                .gender(gender)
//                .level(1)
//                .birthDate(birthDate)
//                .emailVerified(true)
//                .build();
//        return userRepository.save(user);
//    }
//
//    private Schedule createSchedule(Long artistId, String title, ScheduleCategory category,
//                                    LocalDateTime scheduleTime, String location) {
//        Schedule schedule = Schedule.builder()
//                .artistId(artistId)
//                .performanceId(100L + artistId)
//                .title(title)
//                .scheduleCategory(category)
//                .link("https://example.com/schedule/" + title)
//                .scheduleTime(scheduleTime)
//                .location(location)
//                .build();
//        return scheduleRepository.save(schedule);
//    }
//
//    private Party createParty(Long scheduleId, Long leaderId, PartyType partyType, String partyName,
//                              String description, String departureLocation, String arrivalLocation,
//                              TransportType transportType, Integer maxMembers, Gender preferredGender,
//                              PreferredAge preferredAge) {
//        Party party = Party.builder()
//                .scheduleId(scheduleId)
//                .leaderId(leaderId)
//                .partyType(partyType)
//                .partyName(partyName)
//                .description(description)
//                .departureLocation(departureLocation)
//                .arrivalLocation(arrivalLocation)
//                .transportType(transportType)
//                .maxMembers(maxMembers)
//                .preferredGender(preferredGender)
//                .preferredAge(preferredAge)
//                .build();
//        return partyRepository.save(party);
//    }
//
//    private PartyMember createPartyMember(Long partyId, Long userId, MemberRole role) {
//        PartyMember member = role == MemberRole.LEADER
//                ? PartyMember.createLeader(partyId, userId)
//                : PartyMember.createMember(partyId, userId);
//        return partyMemberRepository.save(member);
//    }
//
//    private PartyApplication createPartyApplication(Long partyId, Long applicantId, Long leaderId,
//                                                    ApplicationStatus status) {
//        PartyApplication application = PartyApplication.create(partyId, applicantId, leaderId);
//        return partyApplicationRepository.save(application);
//    }
//}