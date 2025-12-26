package back.kalender.domain.performance.performance.service;

import back.kalender.domain.performance.performance.dto.response.PerformanceDetailResponse;
import back.kalender.domain.performance.performance.entity.Performance;
import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.artist.repository.ArtistRepository;
import back.kalender.domain.performance.performanceHall.entity.PerformanceHall;
import back.kalender.domain.performance.performanceHall.repository.PerformanceHallRepository;
import back.kalender.domain.performance.schedule.entity.PerformanceSchedule;
import back.kalender.domain.performance.priceGrade.entity.PriceGrade;
import back.kalender.domain.performance.performance.repository.PerformanceRepository;
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
public class PerformanceService {
    private final PerformanceRepository performanceRepository;
    private final PerformanceHallRepository performanceHallRepository;
    private final ArtistRepository artistRepository;
    private final PriceGradeRepository priceGradeRepository;
    private final PerformanceScheduleRepository performanceScheduleRepository;

    public PerformanceDetailResponse getPerformanceDetail(Long performanceId) {
        // 공연 정보 조회
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> {
                    log.error("[Performance] [GetDetail] 공연을 찾을 수 없음 - performanceId={}", performanceId);
                    return new ServiceException(ErrorCode.PERFORMANCE_NOT_FOUND);
                });

        // 공연장 정보 조회
        PerformanceHall performanceHall = performanceHallRepository.findById(performance.getPerformanceHallId())
                .orElseThrow(() -> {
                    log.error("[Performance] [GetDetail] 공연장 정보를 찾을 수 없음 - performanceHallId={}",
                            performance.getPerformanceHallId());
                    return new ServiceException(ErrorCode.PERFORMANCE_HALL_NOT_FOUND);
                });

        // 아티스트 정보 조회
        Artist artist = artistRepository.findById(performance.getArtistId())
                .orElseThrow(() -> {
                    log.error("[Performance] [GetDetail] 아티스트 정보를 찾을 수 없음 - artistId={}",
                            performance.getArtistId());
                    return new ServiceException(ErrorCode.ARTIST_NOT_FOUND);
                });

        // 가격 등급 정보 조회
        List<PriceGrade> priceGrades = priceGradeRepository.findAllByPerformanceId(performanceId);

        // 예매 가능한 날짜 목록 조회
        List<LocalDate> availableDates = performanceScheduleRepository.findAvailableDatesByPerformanceId(performanceId);

        // 모든 회차 정보 조회
        List<PerformanceSchedule> schedules = performanceScheduleRepository
                .findAllByPerformanceIdOrderByPerformanceDateAscStartTimeAsc(performanceId);

        // 응답 DTO 생성 및 반환
        return PerformanceDetailResponse.from(performance, performanceHall, artist, priceGrades, availableDates, schedules);
    }
}
