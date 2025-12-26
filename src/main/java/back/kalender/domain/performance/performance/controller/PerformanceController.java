package back.kalender.domain.performance.performance.controller;

import back.kalender.domain.performance.performance.dto.response.PerformanceDetailResponse;

import back.kalender.domain.performance.performance.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/performance")
@RequiredArgsConstructor
public class PerformanceController implements PerformanceControllerSpec {

    private final PerformanceService performanceService;

    @GetMapping("/{performanceId}")
    @Override
    public ResponseEntity<PerformanceDetailResponse> getPerformanceDetail(
            @PathVariable Long performanceId
    ){
        PerformanceDetailResponse response = performanceService.getPerformanceDetail(performanceId);
        return ResponseEntity.ok(response);
    }
}
