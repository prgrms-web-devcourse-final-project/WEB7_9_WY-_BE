package back.kalender.domain.booking.performanceSeat.service;

import back.kalender.domain.booking.performanceSeat.dto.BlockSummaryResponse;
import back.kalender.domain.booking.performanceSeat.dto.SeatDetailResponse;
import back.kalender.domain.booking.performanceSeat.dto.SubBlockSummaryResponse;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.booking.waitingRoom.service.QueueAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceSeatQueryService {

    private final PerformanceSeatRepository performanceSeatRepository;
    private final QueueAccessService queueAccessService;

    @Transactional(readOnly = true)
    public List<BlockSummaryResponse> getBlockSummaries(
            Long scheduleId,
            String bookingSessionId
    ) {
        queueAccessService.checkSeatAccess(scheduleId, bookingSessionId);

        return performanceSeatRepository.findBlockSummaries(scheduleId);
    }

    @Transactional(readOnly = true)
    public List<SubBlockSummaryResponse> getSubBlockSummaries(
            Long scheduleId,
            String block,
            String bookingSessionId
    ) {
        queueAccessService.checkSeatAccess(scheduleId, bookingSessionId);

        return performanceSeatRepository.findSubBlockSummaries(scheduleId, block);
    }

    @Transactional(readOnly = true)
    public List<SeatDetailResponse> getSeatDetails(
            Long scheduleId,
            String block,
            String subBlock,
            String bookingSessionId
    ) {
        queueAccessService.checkSeatAccess(scheduleId, bookingSessionId);

        return performanceSeatRepository.findSeatDetails(scheduleId, block, subBlock);
    }


}
