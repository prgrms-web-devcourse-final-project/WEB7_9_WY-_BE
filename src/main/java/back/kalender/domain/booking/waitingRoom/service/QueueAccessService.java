package back.kalender.domain.booking.waitingRoom.service;

import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class QueueAccessService {

    private final RedisTemplate<String, String> redisTemplate;

    private String activeKey(Long scheduleId) {
        return "active:" + scheduleId; // ZSet member: bookingSessionId
    }

    public void checkSeatAccess(Long scheduleId, String bookingSessionId) {
        boolean allowed = redisTemplate.opsForZSet()
                .score(activeKey(scheduleId), bookingSessionId) != null;

        if (!allowed) {
            throw new ServiceException(ErrorCode.BOOKING_SESSION_EXPIRED);
        }
    }

    public void ping(Long scheduleId, String bookingSessionId) {
        String key = activeKey(scheduleId);

        Double score = redisTemplate.opsForZSet().score(key, bookingSessionId);
        if (score == null) {
            throw new ServiceException(ErrorCode.INVALID_BOOKING_SESSION);
        }

        redisTemplate.opsForZSet().add(key, bookingSessionId, System.currentTimeMillis());
    }
}