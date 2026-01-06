package back.kalender.domain.booking.performanceSeat.controller;

import back.kalender.domain.booking.performanceSeat.dto.BlockSummaryResponse;
import back.kalender.domain.booking.performanceSeat.dto.SeatDetailResponse;
import back.kalender.domain.booking.performanceSeat.dto.SubBlockSummaryResponse;
import back.kalender.domain.booking.performanceSeat.service.PerformanceSeatQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/performances/{scheduleId}/seats")
public class PerformanceSeatController {

    private final PerformanceSeatQueryService service;

    @GetMapping("/summary")
    public List<BlockSummaryResponse> getBlockSummary(
            @PathVariable Long scheduleId,
            @RequestHeader(value = "X-BOOKING-SESSION-ID") String bookingSessionId
    ) {
        return service.getBlockSummaries(scheduleId, bookingSessionId);
    }

    @GetMapping("/blocks/{block}/sub-blocks")
    public List<SubBlockSummaryResponse> getSubBlockSummary(
            @PathVariable Long scheduleId,
            @PathVariable String block,
            @RequestHeader(value = "X-BOOKING-SESSION-ID") String bookingSessionId
    ) {
        return service.getSubBlockSummaries(scheduleId, block, bookingSessionId);
    }

    @GetMapping("/blocks/{block}/sub-blocks/{subBlock}")
    public List<SeatDetailResponse> getSeatDetails(
            @PathVariable Long scheduleId,
            @PathVariable String block,
            @PathVariable String subBlock,
            @RequestHeader(value = "X-BOOKING-SESSION-ID") String bookingSessionId
    ) {
        return service.getSeatDetails(
                scheduleId, block, subBlock, bookingSessionId
        );
    }
}
