package back.kalender.global.initData.performance;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.artist.repository.ArtistRepository;
import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.performance.hallSeat.entity.HallSeat;
import back.kalender.domain.performance.hallSeat.entity.HallSeat.SeatType;
import back.kalender.domain.performance.hallSeat.repository.HallSeatRepository;
import back.kalender.domain.performance.performance.entity.Performance;
import back.kalender.domain.performance.performance.repository.PerformanceRepository;
import back.kalender.domain.performance.performanceHall.entity.PerformanceHall;
import back.kalender.domain.performance.performanceHall.repository.PerformanceHallRepository;
import back.kalender.domain.performance.priceGrade.entity.PriceGrade;
import back.kalender.domain.performance.priceGrade.repository.PriceGradeRepository;
import back.kalender.domain.performance.schedule.entity.PerformanceSchedule;
import back.kalender.domain.performance.schedule.entity.ScheduleStatus;
import back.kalender.domain.performance.schedule.repository.PerformanceScheduleRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class BaseInitData {

    private final PerformanceHallRepository hallRepository;
    private final HallSeatRepository hallSeatRepository;
    private final PerformanceRepository performanceRepository;
    private final PriceGradeRepository priceGradeRepository;
    private final PerformanceScheduleRepository scheduleRepository;
    private final ArtistRepository artistRepository;
    private final PerformanceSeatRepository performanceSeatRepository;

    @PostConstruct
    public void init() {
        initBaseData();          // 설계도
        initPerformanceSeats(); // 파생 데이터
    }

    /* ==================================================
       1️⃣ 설계도 데이터 (공연장, 좌석, 공연, 회차)
       ================================================== */
    private void initBaseData() {
        if (hallRepository.count() > 0) {
            System.out.println("Base data already initialized");
            return;
        }

        // 1. 공연장
        PerformanceHall hall = hallRepository.save(
                new PerformanceHall(
                        "KPOP Arena Hall",
                        "서울 송파구 올림픽로 25",
                        "지하철 2호선 ○○역 4번출구 도보 5분"
                )
        );

        // 2. 아티스트
        Artist artist = artistRepository.save(
                new Artist("KPOP ARTIST", "imageurl:https://example.com")
        );

        // 3. 공연장 좌석
        List<HallSeat> hallSeats = new ArrayList<>();
        createSeatsForFloor(hallSeats, hall, 1, 40, 30, 5);
        createSeatsForFloor(hallSeats, hall, 2, 35, 28, 5);
        createSeatsForFloor(hallSeats, hall, 3, 30, 27, 5);

        hallSeatRepository.saveAll(hallSeats);
        System.out.println("HallSeat created: " + hallSeats.size());

        // 4. 공연
        Performance performance = performanceRepository.save(
                new Performance(
                        hall.getId(),
                        artist.getId(),
                        "KPOP DREAM CONCERT",
                        "https://image.com/poster.jpg",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 10),
                        120,
                        "예매 관련 안내문입니다.",
                        LocalDateTime.now().minusDays(1),
                        LocalDateTime.now().plusDays(10)
                )
        );

        // 5. 가격 등급
        priceGradeRepository.save(new PriceGrade(performance.getId(), "VIP", 200_000));
        priceGradeRepository.save(new PriceGrade(performance.getId(), "R",   150_000));
        priceGradeRepository.save(new PriceGrade(performance.getId(),"S",   100_000));
        priceGradeRepository.save(new PriceGrade(performance.getId(), "A",    70_000));

        // 6. 회차
        scheduleRepository.save(new PerformanceSchedule(
                performance.getId(), LocalDate.of(2026,1,5), LocalTime.of(18,0), 1, ScheduleStatus.AVAILABLE
        ));
        scheduleRepository.save(new PerformanceSchedule(
                performance.getId(), LocalDate.of(2026,1,6), LocalTime.of(18,0), 2, ScheduleStatus.AVAILABLE
        ));
        scheduleRepository.save(new PerformanceSchedule(
                performance.getId(), LocalDate.of(2026,1,7), LocalTime.of(18,0), 3, ScheduleStatus.AVAILABLE
        ));

        System.out.println("Base data initialized");
    }

    /* ==================================================
       2️⃣ PerformanceSeat 전용 초기화 (init2)
       ================================================== */
    private void initPerformanceSeats() {
        if (performanceSeatRepository.count() > 0) {
            System.out.println("PerformanceSeat already initialized");
            return;
        }

        List<PerformanceSchedule> schedules = scheduleRepository.findAll();
        List<HallSeat> hallSeats = hallSeatRepository.findAll();

        if (schedules.isEmpty() || hallSeats.isEmpty()) {
            System.out.println("Skip PerformanceSeat init - base data missing");
            return;
        }

        // 학습용: 모든 좌석을 VIP로 매핑
        Long defaultPriceGradeId =
                priceGradeRepository.findAll().get(0).getId();

        List<PerformanceSeat> performanceSeats = new ArrayList<>();

        for (PerformanceSchedule schedule : schedules) {
            for (HallSeat hallSeat : hallSeats) {
                performanceSeats.add(
                        PerformanceSeat.create(
                                schedule.getId(),
                                hallSeat.getId(),
                                defaultPriceGradeId,
                                hallSeat.getFloor(),
                                hallSeat.getBlock(),
                                hallSeat.getRowNumber(),
                                hallSeat.getSeatNumber(),
                                hallSeat.getX(),
                                hallSeat.getY()
                        )
                );
            }
        }

        performanceSeatRepository.saveAll(performanceSeats);

        System.out.println("PerformanceSeat created: " + performanceSeats.size());
    }

    /* ==================================================
       좌석 생성 유틸
       ================================================== */
    private void createSeatsForFloor(
            List<HallSeat> seats,
            PerformanceHall hall,
            int floor,
            int maxRows,
            int maxNumbers,
            int blockCount
    ) {
        String[] blockNames = {"A", "B", "C", "D", "E"};

        for (int b = 0; b < blockCount; b++) {
            String block = blockNames[b];

            for (int row = 1; row <= maxRows; row++) {
                for (int num = 1; num <= maxNumbers; num++) {

                    int x = b * 300 + num * 10;
                    int y = floor * 1000 + row * 20;

                    seats.add(
                            new HallSeat(
                                    hall,
                                    floor,
                                    block,
                                    row,
                                    num,
                                    x,
                                    y,
                                    SeatType.NORMAL
                            )
                    );
                }
            }
        }
    }
}
