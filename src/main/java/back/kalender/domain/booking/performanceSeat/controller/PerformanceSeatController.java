package back.kalender.domain.booking.performanceSeat.controller;

import back.kalender.domain.booking.performanceSeat.dto.PerformanceSeatResponse;
import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.booking.performanceSeat.service.PerformanceSeatQueryService;
import back.kalender.domain.booking.waitingRoom.controller.QueueControllerSpec;
import back.kalender.domain.booking.waitingRoom.service.QueueAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/performance-seats")
public class PerformanceSeatController implements PerformanceSeatControllerSpec {

    private final PerformanceSeatQueryService performanceSeatQueryService;
    private final QueueAccessService queueAccessService;

    @GetMapping("/schedules/{scheduleId}")
    public List<PerformanceSeatResponse> getPerformanceSeats(
            @PathVariable Long scheduleId,
            @RequestHeader("X-Device-Id") String deviceId
    ) {
        //queueAccessService.checkSeatAccess(scheduleId, deviceId);

        return performanceSeatQueryService.getSeatsByScheduleId(scheduleId, deviceId);
    }
}
