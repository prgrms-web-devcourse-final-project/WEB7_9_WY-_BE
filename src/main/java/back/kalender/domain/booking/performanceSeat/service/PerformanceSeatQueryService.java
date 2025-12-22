package back.kalender.domain.booking.performanceSeat.service;

import back.kalender.domain.booking.performanceSeat.dto.PerformanceSeatResponse;
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

    @Cacheable(
            cacheNames = "seatLayout",
            key = "#scheduleId"
    )
    @Transactional(readOnly = true)
    public List<PerformanceSeatResponse> getSeatsByScheduleId(
            Long scheduleId,
            String qsid
    ) {
        queueAccessService.checkSeatAccess(scheduleId, qsid);

        return performanceSeatRepository.findSeatResponses(scheduleId);
    }
}
