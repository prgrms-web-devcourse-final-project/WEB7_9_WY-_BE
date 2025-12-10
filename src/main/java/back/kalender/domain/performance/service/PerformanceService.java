package back.kalender.domain.performance.service;

import back.kalender.domain.performance.dto.response.PerformanceDetailResponse;

public interface PerformanceService {
    PerformanceDetailResponse getPerformanceDetail(Long performanceId);
}
