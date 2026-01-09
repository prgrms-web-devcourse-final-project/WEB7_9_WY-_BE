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
@RequestMapping("/api/v1/performances")
public class PerformanceSeatStructureController {

    private final PerformanceSeatQueryService performanceSeatQueryService;

    // 1) 블록 요약
    @GetMapping("/{scheduleId}/seats/summary")
    public List<BlockSummaryResponse> getBlockSummaries(
            @PathVariable Long scheduleId,
            @RequestHeader(value = "X-BOOKING-SESSION-ID", required = false) String bookingSessionId
    ) {
        return performanceSeatQueryService.getBlockSummaries(scheduleId, bookingSessionId);
    }

    // 2) 서브블록 요약
    @GetMapping("/{scheduleId}/seats/blocks/{block}/sub-blocks")
    public List<SubBlockSummaryResponse> getSubBlockSummaries(
            @PathVariable Long scheduleId,
            @PathVariable String block,
            @RequestHeader(value = "X-BOOKING-SESSION-ID", required = false) String bookingSessionId
    ) {
        return performanceSeatQueryService.getSubBlockSummaries(scheduleId, block, bookingSessionId);
    }

    // 3) 좌석 상세
    @GetMapping("/{scheduleId}/seats/blocks/{block}/sub-blocks/{subBlock}")
    public List<SeatDetailResponse> getSeatDetails(
            @PathVariable Long scheduleId,
            @PathVariable String block,
            @PathVariable String subBlock,
            @RequestHeader(value = "X-BOOKING-SESSION-ID", required = false) String bookingSessionId
    ) {
        return performanceSeatQueryService.getSeatDetails(scheduleId, block, subBlock, bookingSessionId);
    }
}
