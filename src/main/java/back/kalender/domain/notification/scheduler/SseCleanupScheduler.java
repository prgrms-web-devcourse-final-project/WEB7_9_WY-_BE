package back.kalender.domain.notification.scheduler;

import back.kalender.domain.notification.repository.EmitterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SseCleanupScheduler {

    private final EmitterRepository emitterRepository;

    @Scheduled(fixedRate = 60000)
    public void runCleanup() {
        emitterRepository.deleteExpiredEventCache();
    }
}