package back.kalender.global.initData.party;

import back.kalender.domain.chat.entity.ChatMessage;
import back.kalender.domain.chat.entity.ChatRoom;
import back.kalender.domain.chat.repository.ChatMessageRepository;
import back.kalender.domain.chat.repository.ChatRoomRepository;
import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.entity.PartyMember;
import back.kalender.domain.party.enums.PartyStatus;
import back.kalender.domain.party.enums.PartyType;
import back.kalender.domain.party.enums.PreferredAge;
import back.kalender.domain.party.enums.TransportType;
import back.kalender.domain.party.repository.PartyMemberRepository;
import back.kalender.domain.party.repository.PartyRepository;
import back.kalender.domain.schedule.entity.Schedule;
import back.kalender.domain.schedule.enums.ScheduleCategory;
import back.kalender.domain.schedule.repository.ScheduleRepository;
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import back.kalender.global.common.enums.Gender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile({"prod", "dev"})
@Order(7)
@RequiredArgsConstructor
@Slf4j
public class PartyBaseInitData implements ApplicationRunner {

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (partyRepository.count() > 0) {
            log.info("Party base data already initialized");
            return;
        }
        createParties();
    }

    private void createParties() {
        List<User> users = userRepository.findAll();
        List<Schedule> schedules = scheduleRepository.findAll();

        if (users.isEmpty() || schedules.isEmpty()) {
            log.warn("Not enough users or schedules to create parties");
            log.warn("Users: {}, Schedules: {}", users.size(), schedules.size());
            return;
        }

        if (users.size() < 10) {
            log.warn("Too few users ({}) to create diverse parties. Need at least 10 users.", users.size());
            return;
        }

        List<PartySeed> seeds = createPartySeedList();

        // 상태별 파티 비율 설정 (총 개수를 더 늘림)
        List<PartyStatus> statusList = new ArrayList<>();
        for (int i = 0; i < 300; i++) statusList.add(PartyStatus.RECRUITING);  // 60%
        for (int i = 0; i < 100; i++) statusList.add(PartyStatus.CLOSED);      // 20%
        for (int i = 0; i < 70; i++) statusList.add(PartyStatus.COMPLETED);    // 14%
        for (int i = 0; i < 30; i++) statusList.add(PartyStatus.CANCELLED);    // 6%

        int partyCount = 0;
        int statusIndex = 0;
        LocalDateTime now = LocalDateTime.now();

        // 각 스케줄마다 여러 개의 파티 생성
        for (Schedule schedule : schedules) {
            // 스케줄 카테고리에 따라 파티 생성 개수 결정
            int partiesPerSchedule = getPartiesPerSchedule(schedule.getScheduleCategory());

            for (int i = 0; i < partiesPerSchedule; i++) {
                // 리더는 순환하면서 선택
                User leader = users.get(partyCount % users.size());

                // 파티 시드는 순환하면서 선택
                PartySeed seed = seeds.get(partyCount % seeds.size());

                // 파티 상태는 순환하면서 선택
                PartyStatus targetStatus = statusList.get(statusIndex % statusList.size());
                statusIndex++;

                // 과거 일정의 경우 RECRUITING, CLOSED는 COMPLETED로 변경
                if (schedule.getScheduleTime().isBefore(now)) {
                    if (targetStatus == PartyStatus.RECRUITING || targetStatus == PartyStatus.CLOSED) {
                        targetStatus = PartyStatus.COMPLETED;
                    }
                }

                // 스케줄 장소를 기반으로 파티의 arrivalLocation 설정
                String arrivalLocation = schedule.getLocation() != null ? schedule.getLocation() : "잠실실내체육관";

                // 파티 생성 (currentMembers는 기본값 1로 시작)
                Party party = Party.builder()
                        .scheduleId(schedule.getId())
                        .leaderId(leader.getId())
                        .partyType(seed.partyType())
                        .partyName(seed.partyName())
                        .description(seed.description())
                        .departureLocation(seed.departureLocation())
                        .arrivalLocation(arrivalLocation)
                        .transportType(seed.transportType())
                        .maxMembers(seed.maxMembers())
                        .preferredGender(seed.preferredGender())
                        .preferredAge(seed.preferredAge())
                        .build();

                // 상태만 변경 (멤버 수는 PartyApplicationBaseInitData에서 조정)
                if (targetStatus != PartyStatus.RECRUITING) {
                    party.changeStatus(targetStatus);
                }

                partyRepository.save(party);

                // 리더를 PartyMember로 추가
                partyMemberRepository.save(
                        PartyMember.createLeader(party.getId(), leader.getId())
                );

                // 채팅방 생성
                chatRoomRepository.save(
                        ChatRoom.create(party.getId(), party.getPartyName())
                );

                // 리더의 JOIN 메시지 생성
                chatMessageRepository.save(
                        ChatMessage.createJoinMessage(party.getId(), leader.getId())
                );

                partyCount++;
            }
        }

        // 통계 출력
        long recruitingCount = partyRepository.findAll().stream()
                .filter(p -> p.getStatus() == PartyStatus.RECRUITING).count();
        long closedCount = partyRepository.findAll().stream()
                .filter(p -> p.getStatus() == PartyStatus.CLOSED).count();
        long completedCount = partyRepository.findAll().stream()
                .filter(p -> p.getStatus() == PartyStatus.COMPLETED).count();
        long cancelledCount = partyRepository.findAll().stream()
                .filter(p -> p.getStatus() == PartyStatus.CANCELLED).count();

        log.info("=".repeat(60));
        log.info("Party base data initialized: {} parties", partyCount);
        log.info("  RECRUITING: {} ({}%)", recruitingCount, recruitingCount * 100 / partyCount);
        log.info("  CLOSED: {} ({}%)", closedCount, closedCount * 100 / partyCount);
        log.info("  COMPLETED: {} ({}%)", completedCount, completedCount * 100 / partyCount);
        log.info("  CANCELLED: {} ({}%)", cancelledCount, cancelledCount * 100 / partyCount);
        log.info("Users available: {}, Schedules available: {}", users.size(), schedules.size());
        log.info("All parties start with 1 member (leader only)");
        log.info("Additional members will be added in PartyApplicationBaseInitData");
        log.info("=".repeat(60));
    }

    /**
     * 스케줄 카테고리에 따라 생성할 파티 개수 결정
     */
    private int getPartiesPerSchedule(ScheduleCategory category) {
        return switch (category) {
            case CONCERT -> 5 + (int)(Math.random() * 6);      // 5~10개 (콘서트는 많은 파티)
            case FAN_MEETING -> 3 + (int)(Math.random() * 3);  // 3~5개
            case FESTIVAL -> 4 + (int)(Math.random() * 4);     // 4~7개
            case FAN_SIGN -> 2 + (int)(Math.random() * 3);     // 2~4개
            case BROADCAST -> 1 + (int)(Math.random() * 2);    // 1~2개
            case AWARD_SHOW -> 3 + (int)(Math.random() * 4);   // 3~6개
            default -> 1;                                       // 기타는 1개
        };
    }

    private List<PartySeed> createPartySeedList() {
        List<PartySeed> seeds = new ArrayList<>();

        // LEAVE 타입 파티 (공연장으로 가는 파티)
        seeds.add(new PartySeed("콘서트 함께 가실 분!", "같이 즐겁게 공연 보고 싶어요", "강남역 3번 출구", TransportType.SUBWAY, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("지하철로 함께 가요", "혼자 가기 심심해서 같이 가실 분 구해요", "홍대입구역", TransportType.SUBWAY, 3, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("택시 같이 타실 분", "택시비 나눠내실 분 찾아요", "강남역", TransportType.TAXI, 4, Gender.MALE, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("카풀 함께 하실 분", "자차로 이동하는데 같이 가요", "수원역", TransportType.CARPOOL, 3, Gender.ANY, PreferredAge.THIRTY, PartyType.LEAVE));
        seeds.add(new PartySeed("버스 타고 같이 가요", "버스로 편하게 같이 가실 분", "신촌역", TransportType.BUS, 4, Gender.FEMALE, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("공연 같이 가실 분~", "처음인데 같이 가요!", "서울역", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("20대 여자만 모집", "또래끼리 편하게 가요", "신림역", TransportType.SUBWAY, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("30대 직장인 파티", "퇴근하고 같이 가실 분", "여의도역", TransportType.SUBWAY, 3, Gender.ANY, PreferredAge.THIRTY, PartyType.LEAVE));
        seeds.add(new PartySeed("학생들 모여라!", "대학생끼리 가요~", "건대입구역", TransportType.SUBWAY, 6, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("남자만 모집합니다", "남자끼리 편하게", "구로디지털단지역", TransportType.SUBWAY, 4, Gender.MALE, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("지방에서 올라와요", "같은 지역 분 찾아요", "용산역", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("택시비 나눠요", "4명만 모집해요", "선릉역", TransportType.TAXI, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("응원봉 들고 가요", "같이 응원해요!", "삼성역", TransportType.SUBWAY, 5, Gender.FEMALE, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("조용히 갈 분만", "말 없이 편하게 가요", "교대역", TransportType.SUBWAY, 3, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("수다 떨면서 가요", "신나게 떠들면서!", "사당역", TransportType.SUBWAY, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("카카오택시 같이 타요", "강남에서 출발", "역삼역", TransportType.TAXI, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("대학로에서 출발", "2호선 타고 가요", "혜화역", TransportType.SUBWAY, 3, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("강북에서 출발!", "북쪽 분들 모여요", "노원역", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("부산에서 올라와요", "KTX 같이 타요", "서울역", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("경기도에서 출발", "GTX 타고 가요", "수서역", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("일찍 도착해요", "MD 같이 사러 가요", "잠실새내역", TransportType.WALK, 3, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("콘서트 처음이에요", "초보 환영해요!", "건대입구역", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("VIP석 가시는 분", "같은 구역 분들", "선릉역", TransportType.TAXI, 3, Gender.ANY, PreferredAge.THIRTY, PartyType.LEAVE));
        seeds.add(new PartySeed("스탠딩석 가요", "체력 좋은 분만!", "홍대입구역", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("공연장 근처 산다", "동네 분 구해요", "석촌역", TransportType.WALK, 3, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("친한 사람만!", "편하게 갈 분", "잠실역", TransportType.SUBWAY, 3, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("오래 기다리실 분", "일찍 가서 MD 쇼핑", "송파역", TransportType.SUBWAY, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("사진 많이 찍을 분", "인생샷 건지러 가요", "종합운동장역", TransportType.SUBWAY, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("영상 같이 찍어요", "틱톡 챌린지!", "강남구청역", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("생일 축하해요!", "생일자 파티", "신논현역", TransportType.SUBWAY, 6, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("외국인 친구랑!", "외국인 환영", "이태원역", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("같은 대학교 분!", "대학생 환영", "신촌역", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("고등학생도 OK", "청소년 환영", "수원역", TransportType.SUBWAY, 6, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("40대 이상만", "연령대 맞춰요", "판교역", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.THIRTY, PartyType.LEAVE));
        seeds.add(new PartySeed("시끄러운 거 좋아요", "신나게 소리지를 분!", "합정역", TransportType.SUBWAY, 6, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("조용히 즐길 분", "차분하게 감상", "망원역", TransportType.SUBWAY, 3, Gender.ANY, PreferredAge.THIRTY, PartyType.LEAVE));
        seeds.add(new PartySeed("첫 공연 가시는 분", "첫공 환영", "성수역", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("덕질 토크 환영", "덕질 이야기 나눠요", "뚝섬역", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("굿즈 교환해요", "굿즈 가져오세요", "건대입구역", TransportType.SUBWAY, 5, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("슬로건 만들어요", "같이 제작", "성신여대입구역", TransportType.SUBWAY, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("점심 먹고 가요", "밥 먹고 출발", "종로3가역", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("커피 마시고 가요", "카페 들러요", "이대역", TransportType.SUBWAY, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));

        // ARRIVE 타입 파티 (공연 후 함께 이동하는 파티)
        seeds.add(new PartySeed("공연 후 회식 파티", "공연 끝나고 같이 밥 먹어요~", "강남역 근처", TransportType.WALK, 6, Gender.ANY, PreferredAge.ANY, PartyType.ARRIVE));
        seeds.add(new PartySeed("공연 후 숙소 같이 가요", "공연 끝나고 숙소 같이 가실 분", "홍대 게스트하우스", TransportType.SUBWAY, 5, Gender.FEMALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("공연 후 2차 가실 분", "공연 후 술 한잔 하실 분~", "근처 포차", TransportType.WALK, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("끝나고 카페 가요", "따뜻한 거 마셔요", "롯데월드몰", TransportType.WALK, 4, Gender.FEMALE, PreferredAge.ANY, PartyType.ARRIVE));
        seeds.add(new PartySeed("끝나고 노래방!", "2차로 노래방 가요", "근처 노래방", TransportType.WALK, 6, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("야식 먹을 분", "치맥 하실 분~", "송파구청역", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.ANY, PartyType.ARRIVE));
        seeds.add(new PartySeed("포토카드 교환해요", "굿즈 교환하실 분", "카페", TransportType.WALK, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("감상평 나눠요", "공연 리뷰 같이 해요", "스타벅스", TransportType.WALK, 4, Gender.ANY, PreferredAge.ANY, PartyType.ARRIVE));
        seeds.add(new PartySeed("심야버스 같이 타요", "밤늦게 귀가하시는 분", "각자 집", TransportType.BUS, 5, Gender.FEMALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("택시 나눠타요", "귀가길 같은 방향", "강남 방면", TransportType.TAXI, 4, Gender.ANY, PreferredAge.ANY, PartyType.ARRIVE));
        seeds.add(new PartySeed("끝나고 산책해요", "한강 산책 가실 분", "석촌호수", TransportType.WALK, 3, Gender.FEMALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("밤샘 각오!", "끝나고 클럽 가요", "강남 클럽", TransportType.TAXI, 6, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("PC방 가실 분", "게임 하실 분", "근처 PC방", TransportType.WALK, 4, Gender.MALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("찜질방 가요", "끝나고 찜질방 각", "24시 찜질방", TransportType.SUBWAY, 5, Gender.FEMALE, PreferredAge.ANY, PartyType.ARRIVE));
        seeds.add(new PartySeed("새벽 라면 먹어요", "끝나고 라면 먹을 분", "24시 분식", TransportType.WALK, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("볼링 치러 가요", "운동하실 분", "볼링장", TransportType.SUBWAY, 6, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("당구 치러 가요", "당구 좋아하시는 분", "당구장", TransportType.WALK, 4, Gender.MALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("보드게임 하실 분", "보드게임 카페", "보드게임카페", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("영화 보러 가요", "심야 영화", "CGV", TransportType.WALK, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("맥주 한잔 하실 분", "가볍게 술 한잔", "호프집", TransportType.WALK, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("삼겹살 먹을 분", "고기 드실 분", "삼겹살집", TransportType.WALK, 6, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("치킨 먹을 분", "치킨 좋아하시는 분", "치킨집", TransportType.WALK, 5, Gender.ANY, PreferredAge.ANY, PartyType.ARRIVE));
        seeds.add(new PartySeed("피자 먹을 분", "피자 드실 분", "피자집", TransportType.WALK, 6, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("떡볶이 먹을 분", "분식 좋아하시는 분", "분식집", TransportType.WALK, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.ARRIVE));

        return seeds;
    }

    private record PartySeed(
            String partyName,
            String description,
            String departureLocation,
            TransportType transportType,
            Integer maxMembers,
            Gender preferredGender,
            PreferredAge preferredAge,
            PartyType partyType
    ) {}
}