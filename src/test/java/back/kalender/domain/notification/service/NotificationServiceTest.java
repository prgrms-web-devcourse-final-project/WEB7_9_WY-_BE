package back.kalender.domain.notification.service;

import back.kalender.domain.notification.entity.Notification;
import back.kalender.domain.notification.entity.NotificationType;
import back.kalender.domain.notification.repository.EmitterRepository;
import back.kalender.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private EmitterRepository emitterRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("알림 구독 성공 (LastEventId 없음)")
    void subscribe_Success() {
        Long userId = 1L;
        String lastEventId = "";

        given(emitterRepository.save(anyString(), any(SseEmitter.class)))
                .willReturn(new SseEmitter());

        SseEmitter result = notificationService.subscribe(userId, lastEventId);

        verify(emitterRepository, times(1)).save(anyString(), any(SseEmitter.class));
    }

    @Test
    @DisplayName("알림 구독 시 유실된 데이터가 있다면 재전송한다 (LastEventId 존재)")
    void subscribe_WithLastEventId_ShouldSendLostData() {
        Long userId = 1L;
        String lastEventId = userId + "_1000"; // 클라이언트는 1000번까지만 받음

        Map<String, Object> events = new ConcurrentHashMap<>();
        events.put(userId + "_1001", "놓친 데이터 1");
        events.put(userId + "_1002", "놓친 데이터 2");

        given(emitterRepository.save(anyString(), any(SseEmitter.class)))
                .willReturn(new SseEmitter());

        given(emitterRepository.findAllEventCacheStartWithByMemberId(String.valueOf(userId)))
                .willReturn(events);

        notificationService.subscribe(userId, lastEventId);

        verify(emitterRepository, times(1)).save(anyString(), any(SseEmitter.class));

        verify(emitterRepository, times(1)).findAllEventCacheStartWithByMemberId(String.valueOf(userId));
    }

    @Test
    @DisplayName("알림 발송 시 연결된 모든 Emitter에게 전송하고 캐시에도 저장한다")
    void send_ShouldBroadcastAndCache() {
        Long userId = 1L;
        String notificationContent = "알림 내용";

        Notification notification = new Notification(userId, NotificationType.APPLY, "제목", notificationContent, "/url");

        Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
        emitters.put(userId + "_1", new SseEmitter());
        emitters.put(userId + "_2", new SseEmitter());

        given(notificationRepository.save(any(Notification.class))).willReturn(notification);
        given(emitterRepository.findAllEmitterStartWithByMemberId(String.valueOf(userId))).willReturn(emitters);

        notificationService.send(userId, NotificationType.APPLY, "제목", notificationContent, "/url");

        verify(notificationRepository, times(1)).save(any(Notification.class));

        verify(emitterRepository, times(2)).saveEventCache(anyString(), any(Notification.class));

        verify(emitterRepository, times(1)).findAllEmitterStartWithByMemberId(String.valueOf(userId));
    }
}