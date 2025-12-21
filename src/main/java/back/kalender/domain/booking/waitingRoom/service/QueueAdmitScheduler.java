package back.kalender.domain.booking.waitingRoom.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueAdmitScheduler {

    private final QueueService queueService;

    @Scheduled(fixedDelay = 1000)
    public void admit() {
        queueService.admitIfCapacity(1L, 50);
    }
}