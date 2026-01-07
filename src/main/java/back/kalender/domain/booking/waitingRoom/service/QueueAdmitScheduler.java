package back.kalender.domain.booking.waitingRoom.service;

import back.kalender.domain.performance.schedule.service.ScheduleQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class QueueAdmitScheduler {

    private final QueueService queueService;
    private final ScheduleQueryService scheduleService;

    @Scheduled(fixedDelay = 1000)
    public void admit() {

        List<Long> openScheduleIds = scheduleService.getOpenScheduleIds();

        for (Long scheduleId : openScheduleIds) {
            int admitted = queueService.admitIfCapacity(scheduleId, 4);
        }
    }
}