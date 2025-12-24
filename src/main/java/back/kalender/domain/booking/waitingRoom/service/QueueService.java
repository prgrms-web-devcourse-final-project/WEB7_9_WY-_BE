package back.kalender.domain.booking.waitingRoom.service;

import back.kalender.domain.booking.waitingRoom.dto.QueueJoinResponse;
import back.kalender.domain.booking.waitingRoom.dto.QueueStatusResponse;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class QueueService {

    private static final Duration JOIN_TTL = Duration.ofMinutes(30);
    private static final Duration WAITING_TOKEN_TTL = Duration.ofMinutes(3);

    private final StringRedisTemplate redis;

    public QueueService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String queueKey(Long scheduleId) {
        return "queue:" + scheduleId;
    }

    private String admittedKey(Long scheduleId) {
        return "admitted:" + scheduleId; // Hash: qsid -> waitingToken
    }

    private String activeKey(Long scheduleId) {
        return "active:" + scheduleId;   // ZSet: bookingSessionId -> lastSeenMillis (B안)
    }

    private String waitingTokenKey(String token) {
        return "waiting:" + token;       // String: token -> qsid:scheduleId (TTL 짧게)
    }

    public QueueJoinResponse join(Long scheduleId, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String qKey = queueKey(scheduleId);
        String deviceKey = "device:" + scheduleId + ":" + deviceId;

        String oldQsid = redis.opsForValue().get(deviceKey);
        if (oldQsid != null) {
            redis.opsForZSet().remove(qKey, oldQsid);
            redis.delete("qsid:" + oldQsid);
            redis.delete(deviceKey);
        }

        String newQsid = UUID.randomUUID().toString();

        redis.opsForZSet().add(qKey, newQsid, System.currentTimeMillis());
        redis.expire(qKey, JOIN_TTL);

        redis.opsForValue().set(
                "qsid:" + newQsid,
                deviceId + ":" + scheduleId,
                JOIN_TTL
        );

        redis.opsForValue().set(deviceKey, newQsid, JOIN_TTL);

        Long rank0 = redis.opsForZSet().rank(qKey, newQsid);
        Long position = rank0 == null ? null : rank0 + 1;

        return new QueueJoinResponse("WAITING", position, newQsid);
    }

    public QueueStatusResponse status(Long scheduleId, String qsid) {
        String aKey = admittedKey(scheduleId);

        Object tokenObj = redis.opsForHash().get(aKey, qsid);
        if (tokenObj != null) {
            return new QueueStatusResponse("ADMITTED", null, tokenObj.toString());
        }

        Long rank0 = redis.opsForZSet().rank(queueKey(scheduleId), qsid);
        if (rank0 == null) {
            return new QueueStatusResponse("NOT_IN_QUEUE", null, null);
        }

        return new QueueStatusResponse("WAITING", rank0 + 1, null);
    }

    public String issueWaitingToken(Long scheduleId, String qsid) {
        String token = "wt_" + UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(
                waitingTokenKey(token),
                qsid + ":" + scheduleId,
                WAITING_TOKEN_TTL
        );
        return token;
    }

    public int admitIfCapacity(Long scheduleId, int maxActive) {
        Long activeCnt = redis.opsForZSet().size(activeKey(scheduleId));
        Long admittedCnt = redis.opsForHash().size(admittedKey(scheduleId));

        long inFlight = (activeCnt == null ? 0 : activeCnt) + (admittedCnt == null ? 0 : admittedCnt);
        long available = maxActive - inFlight;

        if (available <= 0) return 0;

        return admitNext(scheduleId, (int) available);
    }

    public int admitNext(Long scheduleId, int n) {
        String qKey = queueKey(scheduleId);
        String aKey = admittedKey(scheduleId);

        var next = redis.opsForZSet().range(qKey, 0, n - 1);
        if (next == null || next.isEmpty()) return 0;

        int admittedCount = 0;

        for (String qsid : next) {
            String token = issueWaitingToken(scheduleId, qsid);

            // admitted: qsid -> token
            redis.opsForHash().put(aKey, qsid, token);
            redis.expire(aKey, JOIN_TTL);

            // queue에서 제거
            redis.opsForZSet().remove(qKey, qsid);

            admittedCount++;
        }

        return admittedCount;
    }
}