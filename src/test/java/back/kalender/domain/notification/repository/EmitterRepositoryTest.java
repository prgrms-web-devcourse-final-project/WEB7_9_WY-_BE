package back.kalender.domain.notification.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

class EmitterRepositoryTest {

    private final EmitterRepository emitterRepository = new EmitterRepository();
    private final Long DEFAULT_TIMEOUT = 60L * 1000L * 60L;

    @Test
    @DisplayName("새로운 Emitter를 추가한다.")
    void save() {
        Long userId = 1L;
        String emitterId = userId + "_" + System.currentTimeMillis();
        SseEmitter sseEmitter = new SseEmitter(DEFAULT_TIMEOUT);

        emitterRepository.save(emitterId, sseEmitter);

        Map<String, SseEmitter> result = emitterRepository.findAllEmitterStartWithByMemberId(String.valueOf(userId));
        Assertions.assertEquals(1, result.size());
    }

    @Test
    @DisplayName("수신한 이벤트를 캐시에 저장한다.")
    void saveEventCache() {
        Long userId = 1L;
        String eventCacheId = userId + "_" + System.currentTimeMillis();
        Object event = "Test Event Data";

        emitterRepository.saveEventCache(eventCacheId, event);

        Map<String, Object> events = emitterRepository.findAllEventCacheStartWithByMemberId(String.valueOf(userId));
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals("Test Event Data", events.get(eventCacheId));
    }

    @Test
    @DisplayName("어떤 회원이 접속한 모든 Emitter를 찾는다.")
    void findAllEmitterStartWithByMemberId() {
        Long userId = 1L;
        String emitterId1 = userId + "_" + System.currentTimeMillis();
        emitterRepository.save(emitterId1, new SseEmitter(DEFAULT_TIMEOUT));

        String emitterId2 = userId + "_" + (System.currentTimeMillis() + 100);
        emitterRepository.save(emitterId2, new SseEmitter(DEFAULT_TIMEOUT));

        Map<String, SseEmitter> result = emitterRepository.findAllEmitterStartWithByMemberId(String.valueOf(userId));

        Assertions.assertEquals(2, result.size());
    }

    @Test
    @DisplayName("어떤 회원에게 발생한 모든 이벤트를 찾는다 (캐시 조회).")
    void findAllEventCacheStartWithByMemberId() {
        Long userId = 1L;
        String eventCacheId1 = userId + "_" + System.currentTimeMillis();
        String eventCacheId2 = userId + "_" + (System.currentTimeMillis() + 100);

        emitterRepository.saveEventCache(eventCacheId1, "데이터1");
        emitterRepository.saveEventCache(eventCacheId2, "데이터2");

        Map<String, Object> result = emitterRepository.findAllEventCacheStartWithByMemberId(String.valueOf(userId));

        Assertions.assertEquals(2, result.size());
    }

    @Test
    @DisplayName("ID로 Emitter를 삭제한다.")
    void deleteById() {
        Long userId = 1L;
        String emitterId = userId + "_" + System.currentTimeMillis();
        SseEmitter sseEmitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitterRepository.save(emitterId, sseEmitter);

        emitterRepository.deleteById(emitterId);

        Map<String, SseEmitter> result = emitterRepository.findAllEmitterStartWithByMemberId(String.valueOf(userId));
        Assertions.assertEquals(0, result.size());
    }
}