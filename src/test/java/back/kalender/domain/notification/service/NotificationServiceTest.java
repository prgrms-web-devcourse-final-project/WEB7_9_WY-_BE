package back.kalender.domain.notification.service;

import back.kalender.domain.notification.repository.EmitterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @InjectMocks
    private NotificationService notificationService;
    @Mock
    private EmitterRepository emitterRepository;

    @Test
    @DisplayName("구독을 요청하면 SseEmitter를 생성하고 저장한다")
    void subscribe_Success() {
        Long userId = 1L;

        SseEmitter result = notificationService.subscribe(userId);

        assertThat(result).isNotNull();
        verify(emitterRepository, times(1)).save(eq(userId), any(SseEmitter.class));
    }
}