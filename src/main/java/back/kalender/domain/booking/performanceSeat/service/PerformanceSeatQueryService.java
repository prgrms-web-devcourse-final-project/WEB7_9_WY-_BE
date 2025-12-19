package back.kalender.domain.booking.performanceSeat.service;

import back.kalender.domain.booking.performanceSeat.dto.PerformanceSeatResponse;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.booking.waitingRoom.service.QueueAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceSeatQueryService {

    private final PerformanceSeatRepository performanceSeatRepository;
    private final QueueAccessService queueAccessService;

    @Transactional(readOnly = true)
    public List<PerformanceSeatResponse> getSeatsByScheduleId(
            Long scheduleId,
            String deviceId
    ) {
        // ✅ 대기열 통과(Active) 여부 체크
        queueAccessService.checkSeatAccess(scheduleId, deviceId);

        return performanceSeatRepository.findSeatResponses(scheduleId);
    }
}
