package back.kalender.domain.performance.schedule.service;

import back.kalender.domain.performance.schedule.entity.ScheduleStatus;
import back.kalender.domain.performance.schedule.repository.PerformanceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleQueryService {

    private final PerformanceScheduleRepository scheduleRepository;

    public List<Long> getOpenScheduleIds() {
        return scheduleRepository.findScheduleIdsByStatus(ScheduleStatus.AVAILABLE);
    }
}