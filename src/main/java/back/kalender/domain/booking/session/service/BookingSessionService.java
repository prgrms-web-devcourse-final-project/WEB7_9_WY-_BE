package back.kalender.domain.booking.session.service;

import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingSessionService {

    private final StringRedisTemplate redisTemplate;
    private static final String BOOKING_SESSION_KEY_PREFIX = "booking:session:";
    private static final String BOOKING_SESSION_DEVICE_PREFIX = "booking:session:device:";
    private static final Duration BOOKING_SESSION_TTL = Duration.ofMinutes(30);

    /**
     * 대기열 토큰으로 세션 생성
     */
    public String createWithWaitingToken(
            Long userId,
            Long scheduleId,
            String waitingToken,
            String deviceId
    ) {
        // 1. waitingToken 검증
        String qsid = validateWaitingToken(waitingToken, scheduleId);

        // 2. deviceId 검증 (대기열 진입 기기 = 예매 기기)
        validateDevice(qsid, deviceId);

        // 3. 중복 세션 확인
        String existingSession = checkExistingSession(userId, scheduleId, deviceId);
        if (existingSession != null) {
            return existingSession;
        }

        // 4. BookingSession 생성
        String bookingSessionId = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                BOOKING_SESSION_KEY_PREFIX + bookingSessionId,
                scheduleId.toString(),
                BOOKING_SESSION_TTL
        );

        redisTemplate.opsForValue().set(
                BOOKING_SESSION_DEVICE_PREFIX + bookingSessionId,
                deviceId,
                BOOKING_SESSION_TTL
        );

        redisTemplate.opsForValue().set(
                BOOKING_SESSION_KEY_PREFIX + userId + ":" + scheduleId,
                bookingSessionId,
                BOOKING_SESSION_TTL
        );

        // 5. Active 추가
        redisTemplate.opsForZSet().add(
                "active:" + scheduleId,
                bookingSessionId,
                System.currentTimeMillis()
        );

        log.info("[BookingSession] 생성 + Active 진입 - userId={}, scheduleId={}, sessionId={}",
                userId, scheduleId, bookingSessionId);

        // 6. waitingToken 소비
        redisTemplate.delete("waiting:" + waitingToken);

        return bookingSessionId;
    }

    /**
     * Ping - Active 유지
     */
    public void ping(Long scheduleId, String bookingSessionId) {
        String activeKey = "active:" + scheduleId;

        Double score = redisTemplate.opsForZSet().score(activeKey, bookingSessionId);
        if (score == null) {
            throw new ServiceException(ErrorCode.NOT_IN_ACTIVE);
        }

        redisTemplate.opsForZSet().add(
                activeKey,
                bookingSessionId,
                System.currentTimeMillis()
        );
    }

    /**
     * Active 명시적 제거
     */
    public void leaveActive(Long scheduleId, String bookingSessionId) {
        redisTemplate.opsForZSet().remove(
                "active:" + scheduleId,
                bookingSessionId
        );

        log.info("[BookingSession] Active 이탈 - sessionId={}", bookingSessionId);
    }

    /**
     * 대기열 토큰 검증
     */
    private String validateWaitingToken(String waitingToken, Long scheduleId) {
        String waitingKey = "waiting:" + waitingToken;
        String value = redisTemplate.opsForValue().get(waitingKey);

        if (value == null) {
            throw new ServiceException(ErrorCode.INVALID_WAITING_TOKEN);
        }

        // value = "qsid:scheduleId"
        String[] parts = value.split(":");
        String qsid = parts[0];
        Long tokenScheduleId = Long.parseLong(parts[1]);

        if (!tokenScheduleId.equals(scheduleId)) {
            throw new ServiceException(ErrorCode.SCHEDULE_MISMATCH);
        }

        return qsid;
    }


    /**
     * 예매창 체류권 검증 (공통)
     * - bookingSessionId 존재 + TTL 유효 여부만 확인
     */
    public void validateExists(String bookingSessionId) {
        if (bookingSessionId == null || bookingSessionId.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_BOOKING_SESSION);
        }

        String key = BOOKING_SESSION_KEY_PREFIX + bookingSessionId;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            throw new ServiceException(ErrorCode.BOOKING_SESSION_EXPIRED);
        }

    }

    /**
     * 예매 시작 시점 검증 (createReservation 전용)
     * - bookingSessionId가 특정 scheduleId용으로 발급된 세션인지 확인
     */
    public void validateForSchedule(String bookingSessionId, Long scheduleId) {
        if (bookingSessionId == null || bookingSessionId.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_BOOKING_SESSION);
        }

        String key = BOOKING_SESSION_KEY_PREFIX + bookingSessionId;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            throw new ServiceException(ErrorCode.BOOKING_SESSION_EXPIRED);
        }

        if (!value.equals(scheduleId.toString())) {
            throw new ServiceException(ErrorCode.INVALID_BOOKING_SESSION);
        }
    }

    /**
     * 예매 완료 or 명시적 종료 시
     *  - Reservation에서 userId, scheduleId를 함께 전달받아 2개 키 모두 삭제
     */
    public void expire(String bookingSessionId,Long userId, Long scheduleId) {
        redisTemplate.delete(BOOKING_SESSION_KEY_PREFIX + bookingSessionId);
        redisTemplate.delete(BOOKING_SESSION_DEVICE_PREFIX + bookingSessionId);
        redisTemplate.delete(BOOKING_SESSION_KEY_PREFIX + userId + ":" + scheduleId);
    }

    private String checkExistingSession(
            Long userId,
            Long scheduleId,
            String deviceId
    ) {
        String mappingKey = BOOKING_SESSION_KEY_PREFIX + userId + ":" + scheduleId;
        String existingSessionId = redisTemplate.opsForValue().get(mappingKey);

        if (existingSessionId == null) {
            return null;
        }

        // 기존 세션의 deviceId 확인
        String sessionDeviceKey = BOOKING_SESSION_DEVICE_PREFIX + existingSessionId;
        String sessionDevice = redisTemplate.opsForValue().get(sessionDeviceKey);

        if (sessionDevice == null) {
            redisTemplate.delete(mappingKey);
            return null;
        }

        if (!sessionDevice.equals(deviceId)) {
            throw new ServiceException(ErrorCode.DEVICE_ALREADY_USED); // 이미 다른 기기로 접속중인 세션이 있습니다
        }

        return existingSessionId;
    }

    // deviceId 검증
    private void validateDevice(String qsid, String deviceId) {
        String qsidKey = "qsid:" + qsid;
        String qsidValue = redisTemplate.opsForValue().get(qsidKey);

        if (qsidValue == null) {
            throw new ServiceException(ErrorCode.QSID_EXPIRED);
        }

        String originalDeviceId = qsidValue.split(":")[0];

        if (!originalDeviceId.equals(deviceId)) {
            log.warn("[BookingSession] 기기 불일치 - qsid={}, expected={}, actual={}",
                    qsid, originalDeviceId, deviceId);
            throw new ServiceException(ErrorCode.DEVICE_ID_MISMATCH);
        }
    }
}
