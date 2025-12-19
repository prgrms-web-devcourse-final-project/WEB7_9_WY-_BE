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

    private String waitingTokenKey(String token) {
        return "waiting:" + token;
    }

    public QueueJoinResponse join(Long scheduleId, String deviceId) {
        String qKey = queueKey(scheduleId);
        String mKey = membersKey(scheduleId);

        Long added = redis.opsForSet().add(mKey, deviceId);
        boolean addedNow = added != null && added == 1L;

        if (addedNow) {
            redis.opsForZSet().add(qKey, deviceId, System.currentTimeMillis());

            redis.expire(qKey, JOIN_TTL);
            redis.expire(mKey, JOIN_TTL);
        }

        Long rank0 = redis.opsForZSet().rank(qKey, deviceId);
        Long position = (rank0 == null) ? null : rank0 + 1;

        return new QueueJoinResponse("WAITING", position);
    }


    public QueueStatusResponse status(Long scheduleId, String deviceId) {

        String aKey = "admitted:" + scheduleId;

        Object tokenObj = redis.opsForHash().get(aKey, deviceId);
        if (tokenObj != null) {
            return new QueueStatusResponse("ADMITTED", null, tokenObj.toString());
        }

        String qKey = queueKey(scheduleId);

        Long rank0 = redis.opsForZSet().rank(qKey, deviceId);
        if (rank0 == null) {
            return new QueueStatusResponse("NOT_IN_QUEUE", null, null);
        }

        return new QueueStatusResponse("WAITING", rank0 + 1, null);
    }

    public String issueWaitingToken(Long scheduleId, String deviceId) {
        String token = "wt_" + UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(
                waitingTokenKey(token),
                deviceId + ":" + scheduleId,
                WAITING_TOKEN_TTL
        );
        return token;
    }

    public int admitIfCapacity(Long scheduleId, int maxActive) {
        String activeKey = "active:" + scheduleId;

        Long current = redis.opsForSet().size(activeKey);
        long available = maxActive - (current == null ? 0 : current);

        if (available <= 0) return 0;

        return admitNext(scheduleId, (int) available);
    }

    public int admitNext(Long scheduleId, int n) {
        String qKey = queueKey(scheduleId);
        String mKey = membersKey(scheduleId);
        String aKey = "admitted:" + scheduleId;

        // 1) 앞에서 n명 뽑기
        var next = redis.opsForZSet().range(qKey, 0, n - 1);
        if (next == null || next.isEmpty()) return 0;

        int admittedCount = 0;

        for (String deviceId : next) {
            // 2) 토큰 발급
            String token = issueWaitingToken(scheduleId, deviceId);

            redis.opsForZSet().add(
                    "active:" + scheduleId,
                    deviceId,
                    System.currentTimeMillis()
            );

            // 3) admitted 기록: deviceId -> token
            redis.opsForHash().put(aKey, deviceId, token);
            redis.expire(aKey, JOIN_TTL); // admitted도 같이 정리되게 TTL(선택)

            // 4) queue에서 제거
            redis.opsForZSet().remove(qKey, deviceId);
            redis.opsForSet().remove(mKey, deviceId);

            admittedCount++;
        }

        return admittedCount;
    }
}
