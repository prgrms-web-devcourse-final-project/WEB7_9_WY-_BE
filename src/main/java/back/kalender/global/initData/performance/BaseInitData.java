package back.kalender.global.initData.performance;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.artist.repository.ArtistRepository;
import back.kalender.domain.performance.performanceHall.entity.PerformanceHall;
import back.kalender.domain.performance.performanceHall.repository.PerformanceHallRepository;
import back.kalender.domain.performance.hallSeat.entity.HallSeat;
import back.kalender.domain.performance.hallSeat.entity.HallSeat.SeatType;
import back.kalender.domain.performance.hallSeat.repository.HallSeatRepository;
import back.kalender.domain.performance.performance.entity.Performance;
import back.kalender.domain.performance.performance.repository.PerformanceRepository;
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

@Profile("dev") // 운영환경 실행 방지
@Component
@RequiredArgsConstructor
public class BaseInitData {

    private final PerformanceHallRepository hallRepository;
    private final HallSeatRepository hallSeatRepository;
    private final PerformanceRepository performanceRepository;
    private final PriceGradeRepository priceGradeRepository;
    private final PerformanceScheduleRepository scheduleRepository;
    private final ArtistRepository artistRepository;

    @PostConstruct
    public void init() {
        if (hallRepository.count() > 0) {
            System.out.println("BaseInitData skipped - already initialized");
            return;
        }

        // 1) 공연장 생성
        PerformanceHall hall = hallRepository.save(
                new PerformanceHall(
                        "KPOP Arena Hall",
                        "서울 송파구 올림픽로 25",
                        "지하철 2호선 ○○역 4번출구 도보 5분"
                )
        );

        // 2) Artist 생성 (필수!)
        Artist artist = artistRepository.save(new Artist("KPOP ARTIST","imageurl:www.amagon..."));

        // 2) 공연장 좌석 생성 (총 15,000석)
        List<HallSeat> hallSeats = new ArrayList<>();

        createSeatsForFloor(hallSeats, hall, 1, 40, 30, 5);
        createSeatsForFloor(hallSeats, hall, 2, 35, 28, 5);
        createSeatsForFloor(hallSeats, hall, 3, 30, 27, 5);

        hallSeatRepository.saveAll(hallSeats);
        System.out.println("HallSeat created: " + hallSeats.size()); // ≈15,000

        // 3) 공연 생성
        Performance performance = performanceRepository.save(
                new Performance(
                        hall,
                        artist,   // artist는 너가 Entity 연결 후 넣으면 됨
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

        // 4) 가격 등급 생성
        priceGradeRepository.save(new PriceGrade(performance, "VIP", 200000));
        priceGradeRepository.save(new PriceGrade(performance, "R", 150000));
        priceGradeRepository.save(new PriceGrade(performance, "S", 100000));
        priceGradeRepository.save(new PriceGrade(performance, "A", 70000));

        // 5) 회차 3개 생성
        scheduleRepository.save(new PerformanceSchedule(
                performance, LocalDate.of(2026,1,5), LocalTime.of(18,0), 1, ScheduleStatus.AVAILABLE
        ));
        scheduleRepository.save(new PerformanceSchedule(
                performance, LocalDate.of(2026,1,6), LocalTime.of(18,0), 2, ScheduleStatus.AVAILABLE
        ));
        scheduleRepository.save(new PerformanceSchedule(
                performance, LocalDate.of(2026,1,7), LocalTime.of(18,0), 3, ScheduleStatus.AVAILABLE
        ));

        System.out.println("BaseInitData completed successfully");
    }

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
