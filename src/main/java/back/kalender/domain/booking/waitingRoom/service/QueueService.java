package back.kalender.domain.booking.waitingRoom.service;

import back.kalender.domain.booking.waitingRoom.dto.QueueJoinResponse;
import back.kalender.domain.booking.waitingRoom.dto.QueueStatusResponse;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
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

        // Active 용량 체크
        if (hasActiveCapacity(scheduleId, 50)) {
            log.info("[Queue] Active 여유 있음 → 즉시 admit - scheduleId={}", scheduleId);
            return joinAndAdmitImmediately(scheduleId, deviceId);
        }

        // Active 만석 → 기존 대기열 로직
        log.info("[Queue] Active 만석 → 대기열 진입 - scheduleId={}", scheduleId);
        return joinQueue(scheduleId, deviceId);
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

    public void waitingPing(Long scheduleId, String qsid) {
        String waitingKey = "waiting:" + qsid;

        if (!redis.hasKey(waitingKey)) {
            throw new ServiceException(ErrorCode.QSID_EXPIRED);
        }

        // TTL 연장
        redis.expire(waitingKey, 20, TimeUnit.SECONDS);

        // queue ZSET score 갱신 (선택)
        redis.opsForZSet().add(
                "queue:" + scheduleId,
                qsid,
                System.currentTimeMillis()
        );
    }

    private boolean hasActiveCapacity(Long scheduleId, int maxActive) {
        Long activeCnt = redis.opsForZSet().size(activeKey(scheduleId));
        Long admittedCnt = redis.opsForHash().size(admittedKey(scheduleId));

        long inFlight = (activeCnt == null ? 0 : activeCnt)
                + (admittedCnt == null ? 0 : admittedCnt);

        boolean hasCapacity = inFlight < maxActive;

        log.debug("[Queue] 용량 체크 - scheduleId={}, active={}, admitted={}, max={}, hasCapacity={}",
                scheduleId, activeCnt, admittedCnt, maxActive, hasCapacity);

        return hasCapacity;
    }

    // 대기열 없이 바로 입장
    private QueueJoinResponse joinAndAdmitImmediately(Long scheduleId, String deviceId) {
        // 기존 대기열 세션 정리
        cleanupExistingQueueSession(scheduleId, deviceId);

        String qsid = UUID.randomUUID().toString();

        redis.opsForValue().set(
                "qsid:" + qsid,
                deviceId + ":" + scheduleId,
                JOIN_TTL
        );

        redis.opsForValue().set(
                "device:" + scheduleId + ":" + deviceId,
                qsid,
                JOIN_TTL
        );

        // waitingToken 즉시 발급
        String waitingToken = issueWaitingToken(scheduleId, qsid);

        // admitted에 추가 (Queue에는 추가 안 함)
        String aKey = admittedKey(scheduleId);
        redis.opsForHash().put(aKey, qsid, waitingToken);
        redis.expire(aKey, JOIN_TTL);

        log.info("[Queue] 즉시 admit 완료 - scheduleId={}, qsid={}, waitingToken={}",
                scheduleId, qsid, waitingToken);

        return new QueueJoinResponse(
                "ADMITTED",
                0L,           // 순번: 0 (대기 없음)
                qsid,
                waitingToken  // waitingToken 즉시 포함
        );
    }

    // Active 만석일 때 - 기존 대기열 로직
    private QueueJoinResponse joinQueue(Long scheduleId, String deviceId) {
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

        return new QueueJoinResponse("WAITING", position, newQsid, null);
    }

    // 기존 대기열 세션 정리
    private void cleanupExistingQueueSession(Long scheduleId, String deviceId) {
        String deviceKey = "device:" + scheduleId + ":" + deviceId;
        String oldQsid = redis.opsForValue().get(deviceKey);

        if (oldQsid != null) {
            // Queue에서 제거
            redis.opsForZSet().remove(queueKey(scheduleId), oldQsid);

            // Admitted에서 제거
            redis.opsForHash().delete(admittedKey(scheduleId), oldQsid);

            // QSID 삭제
            redis.delete("qsid:" + oldQsid);

            // Device 매핑 삭제
            redis.delete(deviceKey);

            log.debug("[Queue] 기존 세션 정리 - oldQsid={}", oldQsid);
        }
    }
}