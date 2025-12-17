package back.kalender.domain.booking.waitingRoom.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class QueueAccessService {

    private final RedisTemplate<String, String> redisTemplate;

    public void checkSeatAccess(Long scheduleId, String deviceId) {
        Double score = redisTemplate.opsForZSet()
                .score(activeKey(scheduleId), deviceId);

        if (score == null) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "대기열을 통과하지 않았습니다."
            );
        }
    }

    private String activeKey(Long scheduleId) {
        return "active:" + scheduleId;
    }

    /** 핑(heartbeat) */
    public void ping(Long scheduleId, String deviceId) {
        redisTemplate.opsForZSet().add(
                activeKey(scheduleId),
                deviceId,
                System.currentTimeMillis()
        );
    }

    /** 좌석 선택 완료 시 active 제거 */
    public void removeActive(Long scheduleId, String deviceId) {
        redisTemplate.opsForZSet().remove(activeKey(scheduleId), deviceId);
    }

    /** 현재 active 수 */
    public long activeCount(Long scheduleId) {
        Long size = redisTemplate.opsForZSet().size(activeKey(scheduleId));
        return size == null ? 0 : size;
    }
}