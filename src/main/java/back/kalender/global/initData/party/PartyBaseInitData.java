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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile({"prod", "dev"})
@Order(6)
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

        List<PartyStatus> statusList = new ArrayList<>();
        for (int i = 0; i < 180; i++) statusList.add(PartyStatus.RECRUITING);
        for (int i = 0; i < 60; i++) statusList.add(PartyStatus.CLOSED);
        for (int i = 0; i < 45; i++) statusList.add(PartyStatus.COMPLETED);
        for (int i = 0; i < 15; i++) statusList.add(PartyStatus.CANCELLED);

        PartyStatus[] statuses = statusList.toArray(new PartyStatus[0]);

        int partyCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 300; i++) {
            Schedule schedule = schedules.get(i % schedules.size());
            User leader = users.get(i % users.size());
            PartySeed seed = seeds.get(i % seeds.size());
            PartyStatus targetStatus = statuses[i % statuses.length];

            if (schedule.getScheduleTime().isBefore(now)) {
                if (targetStatus == PartyStatus.RECRUITING || targetStatus == PartyStatus.CLOSED) {
                    targetStatus = PartyStatus.COMPLETED;
                }
            }

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

            adjustMembersForStatus(party, targetStatus);

            partyRepository.save(party);

            partyMemberRepository.save(
                    PartyMember.createLeader(party.getId(), leader.getId())
            );

            chatRoomRepository.save(
                    ChatRoom.create(party.getId(), party.getPartyName())
            );

            chatMessageRepository.save(
                    ChatMessage.createJoinMessage(party.getId(), leader.getId())
            );

            partyCount++;
        }

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
        log.info("=".repeat(60));
    }

    private void adjustMembersForStatus(Party party, PartyStatus targetStatus) {
        switch (targetStatus) {
            case RECRUITING -> {
                // 모집 중: 기본 상태 유지 (currentMembers = 1, status = RECRUITING)
            }
            case CLOSED -> {
                // 모집 마감: 상태만 CLOSED로 변경 (인원은 PartyApplicationBaseInitData에서 채움)
                party.changeStatus(PartyStatus.CLOSED);
            }
            case COMPLETED -> {
                // 종료: 다양한 인원으로 설정하고 상태 변경
                int completedMembers = (int)(Math.random() * (party.getMaxMembers() - 1)) + 2;
                for (int i = 1; i < completedMembers; i++) {
                    party.incrementCurrentMembers();
                }
                party.changeStatus(PartyStatus.COMPLETED);
            }
            case CANCELLED -> {
                // 취소: 1명 ~ maxMembers-1 사이의 랜덤 인원
                int cancelledMembers = (int)(Math.random() * (party.getMaxMembers() - 1)) + 1;
                for (int i = 1; i < cancelledMembers; i++) {
                    party.incrementCurrentMembers();
                }
                party.changeStatus(PartyStatus.CANCELLED);
            }
        }
    }

    private List<PartySeed> createPartySeedList() {
        List<PartySeed> seeds = new ArrayList<>();

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
        seeds.add(new PartySeed("친한 사람만!", "편하게 갈 분", "잠실역", "잠실실내체육관", TransportType.SUBWAY, 3, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("오래 기다리실 분", "일찍 가서 MD 쇼핑", "송파역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("사진 많이 찍을 분", "인생샷 건지러 가요", "종합운동장역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("영상 같이 찍어요", "틱톡 챌린지!", "강남구청역", "잠실실내체육관", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("생일 축하해요!", "생일자 파티", "신논현역", "잠실실내체육관", TransportType.SUBWAY, 6, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("외국인 친구랑!", "외국인 환영", "이태원역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("같은 대학교 분!", "대학생 환영", "신촌역", "잠실실내체육관", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("고등학생도 OK", "청소년 환영", "수원역", "잠실실내체육관", TransportType.SUBWAY, 6, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("40대 이상만", "연령대 맞춰요", "판교역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.THIRTY, PartyType.LEAVE));
        seeds.add(new PartySeed("시끄러운 거 좋아요", "신나게 소리지를 분!", "합정역", "잠실실내체육관", TransportType.SUBWAY, 6, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("조용히 즐길 분", "차분하게 감상", "망원역", "잠실실내체육관", TransportType.SUBWAY, 3, Gender.ANY, PreferredAge.THIRTY, PartyType.LEAVE));
        seeds.add(new PartySeed("첫 공연 가시는 분", "첫공 환영", "성수역", "잠실실내체육관", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("덕질 토크 환영", "덕질 이야기 나눠요", "뚝섬역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("굿즈 교환해요", "굿즈 가져오세요", "건대입구역", "잠실실내체육관", TransportType.SUBWAY, 5, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("슬로건 만들어요", "같이 제작", "성신여대입구역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("지하철 타고 가요", "편하게 이동", "광화문역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("버스 타실 분", "버스 함께 탑시다", "사당역", "잠실실내체육관", TransportType.BUS, 5, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("자가용 있어요", "카풀 가능", "분당역", "잠실실내체육관", TransportType.CARPOOL, 4, Gender.ANY, PreferredAge.THIRTY, PartyType.LEAVE));
        seeds.add(new PartySeed("오토바이 태워드려요", "빠르게 이동", "구의역", "잠실실내체육관", TransportType.CARPOOL, 2, Gender.MALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("킥보드 타고 가요", "가까우니까", "잠실나루역", "잠실실내체육관", TransportType.WALK, 3, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("자전거 타고 가요", "운동하면서", "한강진역", "잠실실내체육관", TransportType.WALK, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("도보로 갈 분", "산책하면서", "석촌호수역", "잠실실내체육관", TransportType.WALK, 3, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("리무진 예약했어요", "편하게 가요", "압구정역", "잠실실내체육관", TransportType.TAXI, 8, Gender.ANY, PreferredAge.THIRTY, PartyType.LEAVE));
        seeds.add(new PartySeed("공항버스 타요", "김포공항에서", "김포공항역", "잠실실내체육관", TransportType.BUS, 5, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("KTX 같이 타요", "지방에서 올라와요", "광명역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("새벽 출발해요", "아침 일찍", "강남역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("저녁 출발이에요", "퇴근 후", "회사 근처", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.THIRTY, PartyType.LEAVE));
        seeds.add(new PartySeed("점심 먹고 가요", "밥 먹고 출발", "종로3가역", "잠실실내체육관", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));
        seeds.add(new PartySeed("커피 마시고 가요", "카페 들러요", "이대역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("술 한잔 하고 가요", "가볍게 한잔", "신림역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("노래방 가고 가요", "워밍업", "건대역", "잠실실내체육관", TransportType.SUBWAY, 6, Gender.ANY, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("쇼핑하고 가요", "쇼핑 후 출발", "명동역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("게임하고 가요", "PC방 들러요", "대학로역", "잠실실내체육관", TransportType.SUBWAY, 4, Gender.MALE, PreferredAge.TWENTY, PartyType.LEAVE));
        seeds.add(new PartySeed("목욕탕 가고 가요", "씻고 출발", "왕십리역", "잠실실내체육관", TransportType.SUBWAY, 3, Gender.ANY, PreferredAge.ANY, PartyType.LEAVE));

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
        seeds.add(new PartySeed("볼링 치러 가요", "운동하실 분", "잠실실내체육관", "볼링장", TransportType.SUBWAY, 6, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("당구 치러 가요", "당구 좋아하시는 분", "잠실실내체육관", "당구장", TransportType.WALK, 4, Gender.MALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("콘솔 게임 하실 분", "플스방 가요", "잠실실내체육관", "PS방", TransportType.WALK, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("보드게임 하실 분", "보드게임 카페", "잠실실내체육관", "보드게임카페", TransportType.SUBWAY, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("방탈출 하실 분", "방탈출 예약했어요", "잠실실내체육관", "방탈출카페", TransportType.SUBWAY, 6, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("VR 체험하실 분", "VR 카페 가요", "잠실실내체육관", "VR 카페", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("코인노래방 가요", "1인 노래방", "잠실실내체육관", "코인노래방", TransportType.WALK, 3, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("스크린 골프 치실 분", "골프 좋아하시는 분", "잠실실내체육관", "스크린골프", TransportType.SUBWAY, 4, Gender.ANY, PreferredAge.THIRTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("야구연습장 가요", "타격 연습", "잠실실내체육관", "야구연습장", TransportType.SUBWAY, 5, Gender.MALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("영화 보러 가요", "심야 영화", "잠실실내체육관", "CGV", TransportType.WALK, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("편의점 갈 분", "간단하게 먹어요", "잠실실내체육관", "편의점", TransportType.WALK, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("맥주 한잔 하실 분", "가볍게 술 한잔", "잠실실내체육관", "호프집", TransportType.WALK, 5, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("삼겹살 먹을 분", "고기 드실 분", "잠실실내체육관", "삼겹살집", TransportType.WALK, 6, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("치킨 먹을 분", "치킨 좋아하시는 분", "잠실실내체육관", "치킨집", TransportType.WALK, 5, Gender.ANY, PreferredAge.ANY, PartyType.ARRIVE));
        seeds.add(new PartySeed("피자 먹을 분", "피자 드실 분", "잠실실내체육관", "피자집", TransportType.WALK, 6, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("떡볶이 먹을 분", "분식 좋아하시는 분", "잠실실내체육관", "분식집", TransportType.WALK, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("햄버거 먹을 분", "패스트푸드", "잠실실내체육관", "버거킹", TransportType.WALK, 4, Gender.ANY, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("아이스크림 먹을 분", "디저트 좋아하시는 분", "잠실실내체육관", "배스킨라빈스", TransportType.WALK, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("케이크 먹을 분", "생일 케이크", "잠실실내체육관", "뚜레쥬르", TransportType.WALK, 5, Gender.ANY, PreferredAge.ANY, PartyType.ARRIVE));
        seeds.add(new PartySeed("마카롱 먹을 분", "마카롱 좋아하시는 분", "잠실실내체육관", "마카롱가게", TransportType.WALK, 3, Gender.FEMALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("와플 먹을 분", "와플 드실 분", "잠실실내체육관", "와플가게", TransportType.WALK, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("크레페 먹을 분", "크레페 좋아하시는 분", "잠실실내체육관", "크레페가게", TransportType.WALK, 4, Gender.FEMALE, PreferredAge.TWENTY, PartyType.ARRIVE));
        seeds.add(new PartySeed("붕어빵 먹을 분", "겨울 간식", "잠실실내체육관", "붕어빵 포장마차", TransportType.WALK, 4, Gender.ANY, PreferredAge.ANY, PartyType.ARRIVE));

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