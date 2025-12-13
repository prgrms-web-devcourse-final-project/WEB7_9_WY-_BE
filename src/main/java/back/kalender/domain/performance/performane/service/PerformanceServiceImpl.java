package back.kalender.domain.performance.performane.service;

import back.kalender.domain.performance.performane.dto.response.PerformanceDetailResponse;
import back.kalender.domain.performance.performane.entity.Performance;
import back.kalender.domain.performance.schedule.entity.PerformanceSchedule;
import back.kalender.domain.performance.priceGrade.entity.PriceGrade;
import back.kalender.domain.performance.performane.repository.PerformanceRepository;
import back.kalender.domain.performance.schedule.repository.PerformanceScheduleRepository;
import back.kalender.domain.performance.priceGrade.repository.PriceGradeRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PerformanceServiceImpl implements PerformanceService {
    private final PerformanceRepository performanceRepository;
    private final PriceGradeRepository priceGradeRepository;
    private final PerformanceScheduleRepository performanceScheduleRepository;

    @Override
    public PerformanceDetailResponse getPerformanceDetail(Long performanceId) {
        log.info("[Performance] [GetDetail] 공연 정보 조회 시작 - performanceId={}", performanceId);

        // 공연 정보 조회
        Performance performance = performanceRepository.findByIdWithDetails(performanceId)
                .orElseThrow(() -> {
                    log.error("[Performance] [GetDetail] 공연을 찾을 수 없음 - performanceId={}", performanceId);
                    return new ServiceException(ErrorCode.PERFORMANCE_NOT_FOUND);
                });

        log.debug("[Performance] [GetDetail] 공연 기본 정보 조회 완료 - performanceId={}, title={}, artist={}",
                performanceId, performance.getTitle(), performance.getArtist().getName());


        // 가격 등급 정보 조회
        List<PriceGrade> priceGrades = priceGradeRepository.findAllByPerformance(performance);
        log.debug("[Performance] [GetDetail] 가격 등급 조회 완료 - performanceId={}, priceGradeCount={}",
                performanceId, priceGrades.size());

        // 예매 가능한 날짜 목록 조회
        List<LocalDate> availableDates = performanceScheduleRepository.findAvailableDatesByPerformance(performance);
        log.debug("[Performance] [GetDetail] 예매 가능 날짜 조회 완료 - performanceId={}, availableDatesCount={}",
                performanceId, availableDates.size());

        // 모든 회차 정보 조회
        List<PerformanceSchedule> schedules = performanceScheduleRepository
                .findAllByPerformanceOrderByPerformanceDateAscStartTimeAsc(performance);
        log.debug("[Performance] [GetDetail] 회차 정보 조회 완료 - performanceId={}, schedulesCount={}",
                performanceId, schedules.size());
        log.info("[Performance] [GetDetail] 공연 정보 조회 완료 - performanceId={}, title={}",
                performanceId, performance.getTitle());

        // 응답 DTO 생성 및 반환
        return PerformanceDetailResponse.from(performance, priceGrades, availableDates, schedules);
    }
}
