package back.kalender.domain.notification.scheduler;

import back.kalender.domain.notification.repository.EmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SseCleanupScheduler {

    private final EmitterRepository emitterRepository;

    @Scheduled(fixedRate = 60000)
    public void runCleanup() {
        emitterRepository.deleteExpiredEventCache();
    }

    @Scheduled(fixedRate = 45000)
    public void sendHeartbeat() {
        emitterRepository.sendHeartbeatToAll();
        log.debug("SSE Heartbeat 전송 완료");
    }
}