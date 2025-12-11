//package back.kalender.domain.booking.performanceSeat.service;
//
//import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
//import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
//import back.kalender.domain.performance.hallSeat.entity.HallSeat;
//import back.kalender.domain.performance.hallSeat.repository.HallSeatRepository;
//import back.kalender.domain.performance.priceGrade.entity.PriceGrade;
//import back.kalender.domain.performance.priceGrade.repository.PriceGradeRepository;
//import back.kalender.domain.performance.schedule.entity.PerformanceSchedule;
//import back.kalender.domain.performance.schedule.repository.PerformanceScheduleRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class PerformanceSeatCreateService {
//
//    private final PerformanceScheduleRepository scheduleRepository;
//    private final HallSeatRepository hallSeatRepository;
//    private final PriceGradeRepository priceGradeRepository;
//    private final PerformanceSeatRepository performanceSeatRepository;
//
//    /**
//     * PerformanceSchedule × HallSeat 조합으로 PerformanceSeat 생성
//     */
//    @Transactional
//    public void createPerformanceSeats(Long performanceId) {
//
//        // 공연의 모든 회차
//        List<PerformanceSchedule> schedules = scheduleRepository.findByPerformanceId(performanceId);
//
//        // 공연장 좌석 구조
//        List<HallSeat> hallSeats = hallSeatRepository.findByHallId(
//                schedules.get(0).getPerformance().getPerformanceHall().getId()
//        );
//
//        // 가격 등급
//        List<PriceGrade> priceGrades = priceGradeRepository.findByPerformanceId(performanceId);
//
//        for (PerformanceSchedule schedule : schedules) {
//            for (HallSeat hallSeat : hallSeats) {
//
//                Long assignedPriceGradeId = pickPriceGrade(priceGrades, hallSeat);
//
//                PerformanceSeat seat = PerformanceSeat.create(
//                        schedule.getId(),
//                        hallSeat.getId(),
//                        assignedPriceGradeId,
//                        hallSeat.getFloor(),
//                        hallSeat.getBlock(),
//                        hallSeat.getRow(),
//                        hallSeat.getNumber(),
//                        hallSeat.getX(),
//                        hallSeat.getY()
//                );
//
//                performanceSeatRepository.save(seat);
//            }
//        }
//    }
//
//    /**
//     * 블록/층/행/번호 기반으로 priceGrade 선택 로직
//     * (임시 더미 규칙 → 나중에 개선)
//     */
//    private Long pickPriceGrade(List<PriceGrade> grades, HallSeat seat) {
//        return grades.get(0).getId(); // 임시: 모든 좌석을 첫 번째 등급으로
//    }
//}
