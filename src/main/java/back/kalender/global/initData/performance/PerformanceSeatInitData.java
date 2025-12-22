package back.kalender.global.initData.performance;

import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.performance.hallSeat.entity.HallSeat;
import back.kalender.domain.performance.hallSeat.repository.HallSeatRepository;
import back.kalender.domain.performance.performance.entity.Performance;
import back.kalender.domain.performance.performance.repository.PerformanceRepository;
import back.kalender.domain.performance.priceGrade.entity.PriceGrade;
import back.kalender.domain.performance.priceGrade.repository.PriceGradeRepository;
import back.kalender.domain.performance.schedule.entity.PerformanceSchedule;
import back.kalender.domain.performance.schedule.repository.PerformanceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Order(3)
public class PerformanceSeatInitData implements ApplicationRunner {

    private final PerformanceRepository performanceRepository;
    private final PerformanceScheduleRepository performanceScheduleRepository;
    private final PriceGradeRepository priceGradeRepository;
    private final HallSeatRepository hallSeatRepository;
    private final PerformanceSeatRepository performanceSeatRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (performanceSeatRepository.count() > 0) return;

        // 공연장 좌석 설계도 (14950)
        List<HallSeat> hallSeats =
            hallSeatRepository.findByPerformanceHall_Id(1L);

        for (Performance performance : performanceRepository.findAll()) {

            // 공연 회차
            List<PerformanceSchedule> schedules =
                performanceScheduleRepository.findByPerformanceId(performance.getId());

            // 가격 등급 Map (gradeName 기준)
            Map<String, PriceGrade> gradeMap =
                priceGradeRepository.findByPerformanceId(performance.getId())
                    .stream()
                    .collect(Collectors.toMap(
                        PriceGrade::getGradeName,
                        g -> g
                    ));

            for (PerformanceSchedule schedule : schedules) {
                for (HallSeat seat : hallSeats) {

                    PriceGrade grade = resolveGrade(seat, gradeMap);

                    performanceSeatRepository.save(
                        PerformanceSeat.create(
                            schedule.getId(),
                            seat.getId(),
                            grade.getId(),
                            seat.getFloor(),
                            seat.getBlock(),
                            seat.getRowNumber(),
                            seat.getSeatNumber(),
                            seat.getX(),
                            seat.getY()
                        )
                    );
                }
            }
        }

        System.out.println("✅ PerformanceSeat 생성 완료");
    }

    /**
     * 좌석 위치 기반 가격 등급 분리
     */
    private PriceGrade resolveGrade(
            HallSeat seat,
            Map<String, PriceGrade> gradeMap
    ) {
        if (seat.getFloor() == 1) {
            if ("A".equals(seat.getBlock()) || "B".equals(seat.getBlock())) {
                return gradeMap.get("VIP");
            }
            return gradeMap.get("R");
        }
        if (seat.getFloor() == 2) {
            return gradeMap.get("S");
        }
        return gradeMap.get("A");
    }
}