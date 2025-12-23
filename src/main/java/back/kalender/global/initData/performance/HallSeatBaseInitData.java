package back.kalender.global.initData.performance;

import back.kalender.domain.performance.hallSeat.entity.HallSeat;
import back.kalender.domain.performance.hallSeat.entity.HallSeat.SeatType;
import back.kalender.domain.performance.hallSeat.repository.HallSeatRepository;
import back.kalender.domain.performance.performanceHall.entity.PerformanceHall;
import back.kalender.domain.performance.performanceHall.repository.PerformanceHallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

@Component
@Profile({"prod", "dev"})
@Order(1)
@RequiredArgsConstructor
public class HallSeatBaseInitData implements ApplicationRunner {

    private final HallSeatRepository hallSeatRepository;
    private final PerformanceHallRepository performanceHallRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (hallSeatRepository.count() > 0) return;

        PerformanceHall hall = performanceHallRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PerformanceHall이 먼저 생성되어야 합니다."));

        int seatCount = 0;

        // 1층 10,000석
        for (String block : new String[]{"A", "B", "C", "D"}) {
            for (int row = 1; row <= 50; row++) {
                for (int seat = 1; seat <= 50; seat++) {
                    hallSeatRepository.save(
                            new HallSeat(
                                    hall,
                                    1,
                                    block,
                                    row,
                                    seat,
                                    seat * 20,
                                    row * 20,
                                    SeatType.NORMAL
                            )
                    );
                    seatCount++;
                }
            }
        }

        // 2층 5,000석
        for (String block : new String[]{"E", "F"}) {
            for (int row = 1; row <= 50; row++) {
                for (int seat = 1; seat <= 50; seat++) {
                    hallSeatRepository.save(
                            new HallSeat(
                                    hall,
                                    2,
                                    block,
                                    row,
                                    seat,
                                    seat * 18,
                                    row * 18 + 1200,
                                    SeatType.NORMAL
                            )
                    );
                    seatCount++;
                }
            }
        }

        System.out.println("✅ HallSeat 초기 데이터 생성 완료: " + seatCount + "석");
    }
}