package back.kalender.domain.booking.performanceSeat.controller;

import back.kalender.domain.booking.performanceSeat.dto.PerformanceSeatResponse;
import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.booking.performanceSeat.service.PerformanceSeatQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/performance-seats")
public class PerformanceSeatController {

    private final PerformanceSeatQueryService performanceSeatQueryService;
    private final PerformanceSeatRepository performanceSeatRepository;

    @GetMapping("/schedules/{scheduleId}")
    public List<PerformanceSeatResponse> getPerformanceSeats(
            @PathVariable Long scheduleId
    ) {
        return performanceSeatQueryService.getSeatsByScheduleId(scheduleId);
    }

}
