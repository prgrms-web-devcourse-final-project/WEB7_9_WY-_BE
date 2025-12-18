package back.kalender.domain.booking.waitingRoom.service;

import back.kalender.domain.booking.waitingRoom.dto.QueueJoinResponse;
import back.kalender.domain.booking.waitingRoom.dto.QueueStatusResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class QueueService {

    private static final Duration JOIN_TTL = Duration.ofMinutes(30);     // 대기열 데이터 유지(원하면 조절)
    private static final Duration WAITING_TOKEN_TTL = Duration.ofMinutes(3); // 통과 토큰 유효시간

    private final RedisTemplate<String, String> redis;

    public QueueService(RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    private String queueKey(Long scheduleId) {
        return "queue:" + scheduleId;
    }

    private String membersKey(Long scheduleId) {
        return "queue:members:" + scheduleId;
    }

    private String admittedKey(Long scheduleId) {
        return "admitted:" + scheduleId;
    }
    private String activeKey(Long scheduleId) {
        return "active:" + scheduleId;
    }

    private String waitingTokenKey(String token) {
        return "waiting:" + token;
    }

    public QueueJoinResponse join(Long scheduleId, String deviceId) {
        String qsid = UUID.randomUUID().toString();

        String qKey = queueKey(scheduleId);
        String qsidKey = "qsid:" + qsid;

        redis.opsForZSet().add(qKey, qsid, System.currentTimeMillis());
        redis.expire(qKey, JOIN_TTL);

        redis.opsForValue().set(
                qsidKey,
                deviceId + ":" + scheduleId,
                JOIN_TTL
        );

        Long rank0 = redis.opsForZSet().rank(qKey, qsid);
        Long position = rank0 == null ? null : rank0 + 1;

        return new QueueJoinResponse("WAITING", position, qsid);
    }

    public QueueStatusResponse status(Long scheduleId, String qsid) {

        String admittedKey = admittedKey(scheduleId);

        Object tokenObj = redis.opsForHash().get(admittedKey, qsid);
        if (tokenObj != null) {
            return new QueueStatusResponse("ADMITTED", null, tokenObj.toString());
        }

        Long rank0 = redis.opsForZSet()
                .rank(queueKey(scheduleId), qsid);

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
        String activeKey = activeKey(scheduleId);

        Long current = redis.opsForZSet().size(activeKey);
        long available = maxActive - (current == null ? 0 : current);

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

            redis.opsForZSet().add(
                    activeKey(scheduleId),
                    qsid,
                    System.currentTimeMillis()
            );

            redis.opsForHash().put(aKey, qsid, token);
            redis.expire(aKey, JOIN_TTL);

            redis.opsForZSet().remove(qKey, qsid);

            admittedCount++;
        }

        return admittedCount;
    }
}
