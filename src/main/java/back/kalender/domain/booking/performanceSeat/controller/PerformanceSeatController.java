package back.kalender.domain.booking.performanceSeat.controller;

import back.kalender.domain.booking.performanceSeat.dto.PerformanceSeatResponse;
import back.kalender.domain.booking.performanceSeat.service.PerformanceSeatQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/performance-seats")
public class PerformanceSeatController implements PerformanceSeatControllerSpec {

    private final PerformanceSeatQueryService performanceSeatQueryService;

    @GetMapping("/schedules/{scheduleId}")
    public List<PerformanceSeatResponse> getPerformanceSeats(
            @PathVariable Long scheduleId,
            @RequestHeader("X-BOOKING-SESSION-ID") String bookingSessionId
    ) {
        return performanceSeatQueryService
                .getSeatsByScheduleId(scheduleId, bookingSessionId);
    }
}
