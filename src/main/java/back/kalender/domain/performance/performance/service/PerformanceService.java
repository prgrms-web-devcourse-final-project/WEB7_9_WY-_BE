package back.kalender.domain.performance.performance.service;

import back.kalender.domain.performance.performance.dto.response.PerformanceDetailResponse;

public interface PerformanceService {
    PerformanceDetailResponse getPerformanceDetail(Long performanceId);
}
