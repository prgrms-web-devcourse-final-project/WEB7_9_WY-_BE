package back.kalender.domain.notification.service;

import back.kalender.domain.notification.entity.Notification;
import back.kalender.domain.notification.entity.NotificationType;
import back.kalender.domain.notification.repository.EmitterRepository;
import back.kalender.domain.notification.repository.NotificationRepository;
import back.kalender.domain.notification.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmitterRepository emitterRepository;
    private final NotificationRepository notificationRepository;
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        emitterRepository.save(userId, emitter);

        emitter.onCompletion(() -> emitterRepository.deleteById(userId));
        emitter.onTimeout(() -> emitterRepository.deleteById(userId));

        sendToClient(emitter, userId, "connect", "EventStream 생성됨. [userId=" + userId + "]");

        return emitter;
    }

    @Transactional
    public void send(Long receiverId, NotificationType type, String title, String content, String targetUrl) {
        Notification notification = notificationRepository.save(
                new Notification(receiverId, type, title, content, targetUrl)
        );

        SseEmitter emitter = emitterRepository.get(receiverId);
        if (emitter != null) {
            sendToClient(emitter, receiverId,"notification", NotificationResponse.from(notification));
        }
    }

    private void sendToClient(SseEmitter emitter, Long userId, String eventName, Object data) {
        try {
            String eventId = userId + "_" + System.currentTimeMillis();

            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name(eventName)
                    .data(data));
        } catch (IOException exception) {
            emitterRepository.deleteById(userId);
            log.error("SSE 연결 오류", exception);
        }
    }
}

//eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzY2MDI3Mzk1LCJleHAiOjE3NjYwMjkxOTV9.8IwhDPEQNiTiFTpcMqNHQ8fJwPU5gTu9UAPbRxlbpa0uaflwx-qM2O_e21HvoZR80v0SXsovYNv-ZonkLq7u7w