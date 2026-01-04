package back.kalender.domain.notification.repository;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
@NoArgsConstructor
public class EmitterRepository {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, Object> eventCache = new ConcurrentHashMap<>();

    private static final long EVENT_CACHE_TTL = 10 * 60 * 1000L;

    public SseEmitter save(String emitterId, SseEmitter sseEmitter) {
        emitters.put(emitterId, sseEmitter);
        return sseEmitter;
    }

    public void saveEventCache(String eventCacheId, Object event) {
        eventCache.put(eventCacheId, event);
    }

    public Map<String, SseEmitter> findAllEmitterStartWithByMemberId(String memberId) {
        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(memberId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, Object> findAllEventCacheStartWithByMemberId(String memberId) {
        return eventCache.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(memberId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void deleteById(String emitterId) {
        emitters.remove(emitterId);
    }

    // 유저가 탈퇴하거나, 로그아웃했을 때 해당 유저와 관련된 모든 Emitter를 삭제
    public void deleteAllEmitterStartWithId(String memberId) {
        emitters.forEach((key, emitter) -> {
            if (key.startsWith(memberId)) {
                emitters.remove(key);
            }
        });
    }

    public void deleteExpiredEventCache() {
        long now = System.currentTimeMillis();

        eventCache.keySet().removeIf(key -> {
            try {
                int lastUnderscoreIndex = key.lastIndexOf("_");

                if (lastUnderscoreIndex == -1) {
                    log.warn("잘못된 키 형식 발견 (삭제됨): {}", key);
                    return true;
                }

                String timeString = key.substring(lastUnderscoreIndex + 1);
                long timestamp = Long.parseLong(timeString);

                boolean isExpired = (now - timestamp > EVENT_CACHE_TTL);

                if (isExpired) {
                    log.debug("캐시 만료되어 삭제됨: {}", key);
                }

                return isExpired;

            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                log.warn("파싱 에러로 인한 비정상 데이터 삭제: {}", key);
                return true;
            }
        });
    }

    public void sendHeartbeatToAll() {
        emitters.forEach((key, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(key)
                        .name("heartbeat")
                        .data(""));
            } catch (Exception e) {
                emitters.remove(key);
                log.warn("Heartbeat 전송 실패로 인한 Emitter 삭제: {}", key);
            }
        });
    }
}