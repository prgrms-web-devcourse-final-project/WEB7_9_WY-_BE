package back.kalender.domain.performance.service;

import back.kalender.domain.performance.dto.response.PerformanceDetailResponse;
import back.kalender.domain.performance.entity.Performance;
import back.kalender.domain.performance.entity.PerformanceSchedule;
import back.kalender.domain.performance.entity.PriceGrade;
import back.kalender.domain.performance.repository.PerformanceRepository;
import back.kalender.domain.performance.repository.PerformanceScheduleRepository;
import back.kalender.domain.performance.repository.PriceGradeRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceServiceImpl implements PerformanceService {
    private final PerformanceRepository performanceRepository;
    private final PriceGradeRepository priceGradeRepository;
    private final PerformanceScheduleRepository performanceScheduleRepository;

    @Override
    public PerformanceDetailResponse getPerformanceDetail(Long performanceId) {
        // 공연 정보 조회
        Performance performance = performanceRepository.findByIdWithDetails(performanceId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PERFORMANCE_NOT_FOUND));

        // 가격 등급 정보 조회
        List<PriceGrade> priceGrades = priceGradeRepository.findAllByPerformance(performance);

        // 예매 가능한 날짜 목록 조회
        List<LocalDate> availableDates = performanceScheduleRepository.findAvailableDatesByPerformance(performance);

        // 모든 회차 정보 조회
        List<PerformanceSchedule> schedules = performanceScheduleRepository
                .findAllByPerformanceOrderByPerformanceDateAscStartTimeAsc(performance);

        // 응답 DTO 생성 및 반환
        return PerformanceDetailResponse.from(performance, priceGrades, availableDates, schedules);
    }
}
