package back.kalender.domain.notification.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Repo 로직 단위 테스트")
class EmitterRepositoryTest {
    private final EmitterRepository emitterRepository = new EmitterRepository();

    @Test
    @DisplayName("새로운 Emitter를 저장한다")
    void saveEmitter() {
        Long userId = 1L;
        SseEmitter emitter = new SseEmitter();

        emitterRepository.save(userId, emitter);

        assertThat(emitterRepository.get(userId)).isEqualTo(emitter);
    }

    @Test
    @DisplayName("저장된 Emitter를 삭제한다")
    void deleteEmitter() {
        Long userId = 1L;
        emitterRepository.save(userId, new SseEmitter());

        emitterRepository.deleteById(userId);

        assertThat(emitterRepository.get(userId)).isNull();
    }
}