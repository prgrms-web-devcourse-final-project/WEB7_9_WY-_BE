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

    public void checkSeatAccess(Long scheduleId, String qsid) {
        Boolean allowed = redisTemplate.opsForZSet()
                .score(activeKey(scheduleId), qsid) != null;

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private String activeKey(Long scheduleId) {
        return "active:" + scheduleId;
    }

    public void ping(Long scheduleId, String qsid) {
        redisTemplate.opsForZSet().add(
                activeKey(scheduleId),
                qsid,
                System.currentTimeMillis()
        );
    }

    // B개발자 담당
    public void removeActive(Long scheduleId, String qsid) {
        redisTemplate.opsForZSet().remove(activeKey(scheduleId), qsid);
    }

    //관리자 대시보드에서 사용할 예정
    public long activeCount(Long scheduleId) {
        Long size = redisTemplate.opsForZSet().size(activeKey(scheduleId));
        return size == null ? 0 : size;
    }
}