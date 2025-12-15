package back.kalender.domain.performance.performane.service;

import back.kalender.domain.performance.performane.dto.response.PerformanceDetailResponse;

public interface PerformanceService {
    PerformanceDetailResponse getPerformanceDetail(Long performanceId);
}
