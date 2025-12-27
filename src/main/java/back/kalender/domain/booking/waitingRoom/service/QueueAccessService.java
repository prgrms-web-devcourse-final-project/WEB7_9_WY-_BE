package back.kalender.domain.booking.waitingRoom.service;

import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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

}