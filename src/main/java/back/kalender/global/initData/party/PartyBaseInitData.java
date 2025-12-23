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

import java.util.ArrayList;
import java.util.List;

@Component
@Profile({"prod"})
@Order(6)  // Schedule, User가 먼저 생성되어야 함
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

        // PartyStatus 배열 (골고루 분배)
        PartyStatus[] statuses = {
                PartyStatus.RECRUITING, PartyStatus.RECRUITING, PartyStatus.RECRUITING,
                PartyStatus.RECRUITING, PartyStatus.RECRUITING, PartyStatus.RECRUITING,
                PartyStatus.RECRUITING, PartyStatus.RECRUITING, PartyStatus.RECRUITING,
                PartyStatus.RECRUITING, PartyStatus.RECRUITING, PartyStatus.RECRUITING,
                PartyStatus.CLOSED, PartyStatus.CLOSED, PartyStatus.CLOSED,
                PartyStatus.CLOSED, PartyStatus.CLOSED, PartyStatus.CLOSED,
                PartyStatus.CLOSED, PartyStatus.CLOSED, PartyStatus.CLOSED,
                PartyStatus.CLOSED, PartyStatus.CLOSED, PartyStatus.CLOSED,
                PartyStatus.COMPLETED, PartyStatus.COMPLETED, PartyStatus.COMPLETED,
                PartyStatus.COMPLETED, PartyStatus.COMPLETED, PartyStatus.COMPLETED,
                PartyStatus.COMPLETED, PartyStatus.COMPLETED, PartyStatus.COMPLETED,
                PartyStatus.COMPLETED, PartyStatus.COMPLETED, PartyStatus.COMPLETED,
                PartyStatus.CANCELLED, PartyStatus.CANCELLED, PartyStatus.CANCELLED,
                PartyStatus.CANCELLED, PartyStatus.CANCELLED, PartyStatus.CANCELLED
        };

        int partyCount = 0;

        for (int i = 0; i < seeds.size() && i < statuses.length; i++) {
            Schedule schedule = schedules.get(i % schedules.size());
            User leader = users.get(i % users.size());
            PartySeed seed = seeds.get(i);
            PartyStatus status = statuses[i];

            // Party 생성 (currentMembers = 1, 리더만)
            Party party = Party.builder()
                    .scheduleId(schedule.getId())
                    .leaderId(leader.getId())
                    .partyType(seed.partyType())
                    .partyName(seed.partyName())
                    .description(seed.description())
                    .departureLocation(seed.departureLocation())
                    .arrivalLocation(seed.arrivalLocation())
                    .transportType(seed.transportType())
                    .maxMembers(seed.maxMembers())
                    .preferredGender(seed.preferredGender())
                    .preferredAge(seed.preferredAge())
                    .build();

            party.changeStatus(status);
            partyRepository.save(party);

            // PartyMember (LEADER만) 생성
            partyMemberRepository.save(
                    PartyMember.createLeader(party.getId(), leader.getId())
            );

            // ChatRoom 생성
            chatRoomRepository.save(
                    ChatRoom.create(party.getId(), party.getPartyName())
            );

            // 리더 JOIN 메시지 생성
            chatMessageRepository.save(
                    ChatMessage.createJoinMessage(party.getId(), leader.getId())
            );

            partyCount++;
        }

        log.info("=".repeat(60));
        log.info("Party base data initialized: {} parties with leaders only", partyCount);
        log.info("Users available: {}, Schedules available: {}", users.size(), schedules.size());
        log.info("=".repeat(60));
    }

    private List<PartySeed> createPartySeedList() {
        List<PartySeed> seeds = new ArrayList<>();

        // LEAVE 파티들 (25개)
        seeds.add(new PartySeed("콘서트 함께 가실 분!", "같이 즐겁게 공연 보고 싶어요", "강남역 3번 출구", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("지하철로 함께 가요", "혼자 가기 심심해서 같이 가실 분 구해요", "홍대입구역", "잠실실내체육관", TransportType.SUBWAY, 3, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("택시 같이 타실 분", "택시비 나눠내실 분 찾아요", "강남역", "잠실실내체육관", TransportType.TAXI, 4, Gender.MALE, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("카풀 함께 하실 분", "자차로 이동하는데 같이 가요", "수원역", "잠실실내체육관", TransportType.CARPOOL, 3, Gender.ANY, PreferredAge.THIRTY, PartyType.LEAVE));
        seeds.add(new PartySeed("버스 타고 같이 가요", "버스로 편하게 같이 가실 분", "신촌역", "잠실실내체육관", TransportType.BUS, 4, Gender.FEMALE, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("공연 같이 가실 분~", "처음인데 같이 가요!", "서울역", "잠실실내체육관", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("20대 여자만 모집", "또래끼리 편하게 가요", "신림역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("30대 직장인 파티", "퇴근하고 같이 가실 분", "여의도역", "잠실실내체육관", TransportType.SUBWAY, 3, Gender.ANY, PreferredAge.THIRTY, PartyType.LEAVE));
        seeds.add(new PartySeed("학생들 모여라!", "대학생끼리 가요~", "건대입구역", "잠실실내체육관", TransportType.SUBWAY, 6, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("남자만 모집합니다", "남자끼리 편하게", "구로디지털단지역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.MALE, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("지방에서 올라와요", "같은 지역 분 찾아요", "용산역", "잠실실내체육관", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("택시비 나눠요", "4명만 모집해요", "선릉역", "잠실실내체육관", TransportType.TAXI, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("응원봉 들고 가요", "같이 응원해요!", "삼성역", "잠실실내체육관", TransportType.SUBWAY, 5, Gender.FEMALE, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("조용히 갈 분만", "말 없이 편하게 가요", "교대역", "잠실실내체육관", TransportType.SUBWAY, 3, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("수다 떨면서 가요", "신나게 떠들면서!", "사당역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("카카오택시 같이 타요", "강남에서 출발", "역삼역", "잠실실내체육관", TransportType.TAXI, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("대학로에서 출발", "2호선 타고 가요", "혜화역", "잠실실내체육관", TransportType.SUBWAY, 3, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("강북에서 출발!", "북쪽 분들 모여요", "노원역", "잠실실내체육관", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("부산에서 올라와요", "KTX 같이 타요", "서울역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("경기도에서 출발", "GTX 타고 가요", "수서역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("일찍 도착해요", "MD 같이 사러 가요", "잠실새내역", "잠실실내체육관", TransportType.WALK, 3, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("콘서트 처음이에요", "초보 환영해요!", "건대입구역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("VIP석 가시는 분", "같은 구역 분들", "선릉역", "잠실실내체육관", TransportType.TAXI, 3, Gender.ANY, PreferredAge.THIRTY, PartyType.LEAVE));
        seeds.add(new PartySeed("스탠딩석 가요", "체력 좋은 분만!", "홍대입구역", "잠실실내체육관", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("공연장 근처 산다", "동네 분 구해요", "석촌역", "잠실실내체육관", TransportType.WALK, 3, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));

        // AFTER 파티들 (17개)
        seeds.add(new PartySeed("공연 후 회식 파티", "공연 끝나고 같이 밥 먹어요~", "잠실실내체육관", "잠실역 맛집 거리", TransportType.WALK, 6, Gender.ANY, PreferredAge.ANY, PartyType.ARRIVE));
        seeds.add(new PartySeed("공연 후 숙소 같이 가요", "공연 끝나고 숙소 같이 가실 분", "잠실실내체육관", "홍대 게스트하우스", TransportType.SUBWAY, 5, Gender.FEMALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("공연 후 2차 가실 분", "공연 후 술 한잔 하실 분~", "잠실실내체육관", "잠실 포차거리", TransportType.WALK, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("끝나고 카페 가요", "따뜻한 거 마셔요", "잠실실내체육관", "롯데월드몰", TransportType.WALK, 4, Gender.FEMALE, PreferredAge.ANY, PartyType.ARRIVE));
        seeds.add(new PartySeed("끝나고 노래방!", "2차로 노래방 가요", "잠실실내체육관", "잠실역 노래방", TransportType.WALK, 6, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("야식 먹을 분", "치맥 하실 분~", "잠실실내체육관", "송파구청역", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.ANY, PartyType.ARRIVE));
        seeds.add(new PartySeed("포토카드 교환해요", "굿즈 교환하실 분", "잠실실내체육관", "카페", TransportType.WALK, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("감상평 나눠요", "공연 리뷰 같이 해요", "잠실실내체육관", "스타벅스", TransportType.WALK, 4, Gender.ANY, PreferredAge.ANY, PartyType.ARRIVE));
        seeds.add(new PartySeed("심야버스 같이 타요", "밤늦게 귀가하시는 분", "잠실실내체육관", "각자 집", TransportType.BUS, 5, Gender.FEMALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("택시 나눠타요", "귀가길 같은 방향", "잠실실내체육관", "강남 방면", TransportType.TAXI, 4, Gender.ANY, PreferredAge.ANY, PartyType.ARRIVE));
        seeds.add(new PartySeed("끝나고 산책해요", "한강 산책 가실 분", "잠실실내체육관", "석촌호수", TransportType.WALK, 3, Gender.FEMALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("밤샘 각오!", "끝나고 클럽 가요", "잠실실내체육관", "강남 클럽", TransportType.TAXI, 6, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("숙소 같이 가요", "게스트하우스 예약했어요", "잠실실내체육관", "홍대", TransportType.SUBWAY, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("PC방 가실 분", "게임 하실 분", "잠실실내체육관", "잠실역 PC방", TransportType.WALK, 4, Gender.MALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("찜질방 가요", "끝나고 찜질방 각", "잠실실내체육관", "24시 찜질방", TransportType.SUBWAY, 5, Gender.FEMALE, PreferredAge.ANY, PartyType.ARRIVE));
        seeds.add(new PartySeed("새벽 라면 먹어요", "끝나고 라면 먹을 분", "잠실실내체육관", "24시 분식", TransportType.WALK, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("파티룸 대관했어요", "같이 놀 분 모집", "잠실실내체육관", "강남 파티룸", TransportType.TAXI, 8, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));

        return seeds;
    }

    private record PartySeed(
            String partyName,
            String description,
            String departureLocation,
            String arrivalLocation,
            TransportType transportType,
            Integer maxMembers,
            Gender preferredGender,
            PreferredAge preferredAge,
            PartyType partyType
    ) {}
}