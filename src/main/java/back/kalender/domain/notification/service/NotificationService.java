package back.kalender.domain.notification.service;

import back.kalender.domain.notification.repository.EmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmitterRepository emitterRepository;
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        emitterRepository.save(userId, emitter);

        emitter.onCompletion(() -> {
            log.info("SSE Connection Completed for userId: {}", userId);
            emitterRepository.deleteById(userId);
        });
        emitter.onTimeout(() -> {
            log.info("SSE Connection Timeout for userId: {}", userId);
            emitterRepository.deleteById(userId);
        });

        sendToClient(emitter, userId, "EventStream Created. [userId=" + userId + "]");

        return emitter;
    }

    private void sendToClient(SseEmitter emitter, Long userId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(userId))
                    .name("connect")
                    .data(data));
        } catch (IOException exception) {
            emitterRepository.deleteById(userId);
            log.error("SSE 연결 오류", exception);
        }
    }
}
