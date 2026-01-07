package back.kalender.domain.booking.session.service;

import back.kalender.domain.booking.reservation.service.ReservationService;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
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
    private static final String BOOKING_SESSION_USER_PREFIX = "booking:session:user:";

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
        // (1) 중복 요청 방지 락 (waitingToken 단위)
        String lockKey = "waiting:lock:" + waitingToken;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(locked)) {
            // ErrorCode 없으면 임시로 INVALID_INPUT_VALUE 같은 걸로 막아도 됨
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }

        try {
            // 1) waitingToken 검증 (아직 delete 하지 말 것)
            String qsid = validateWaitingToken(waitingToken, scheduleId);

            // 2) deviceId 검증
            validateDevice(qsid, deviceId);

            // 3) 기존 세션 확인/삭제
            checkExistingSession(userId, scheduleId, deviceId);

            // 4) BookingSession 생성
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

            redisTemplate.opsForValue().set(
                    BOOKING_SESSION_USER_PREFIX + bookingSessionId,
                    userId.toString(),
                    BOOKING_SESSION_TTL
            );

            // 5) Active 추가
            redisTemplate.opsForZSet().add(
                    "active:" + scheduleId,
                    bookingSessionId,
                    System.currentTimeMillis()
            );

            // 6) 여기서 Queue 종료(정리) - 성공했으니 마지막에 삭제
            // waitingToken 소비
            redisTemplate.delete("waiting:" + waitingToken);

            // admitted / qsid / device 매핑 삭제
            redisTemplate.opsForHash().delete("admitted:" + scheduleId, qsid);
            redisTemplate.delete("qsid:" + qsid);
            redisTemplate.delete("device:" + scheduleId + ":" + deviceId);

            log.info("[BookingSession] 생성 + Active 진입 - userId={}, scheduleId={}, sessionId={}",
                    userId, scheduleId, bookingSessionId);

            return bookingSessionId;

        } finally {
            // 락 해제(안전 단순 버전)
            redisTemplate.delete(lockKey);
        }
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
     */
    public void expire(String bookingSessionId, Long userId, Long scheduleId) {
        deleteBookingSession(bookingSessionId, userId, scheduleId);
    }

    /**
     * 기존 세션 확인
     * - 재접속 시 무조건 새로 생성 (브라우저 닫기 = 예매 포기)
     */
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

        log.info("[BookingSession] 기존 세션 발견, 무조건 삭제 후 재생성 - sessionId={}",
                existingSessionId);

        // Active 여부 상관없이 삭제
        deleteBookingSessionBySessionId(existingSessionId);

        return null; // 새로 생성
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

    /** BookingSession 완전 삭제
     * checkExistingSession()에서 비활성 세션 발견 시 호출
     * ActiveSweepScheduler에서 60초 무응답 세션 정리 시 호출
     */
    public boolean deleteBookingSessionBySessionId(String bookingSessionId) {
        String scheduleIdStr = redisTemplate.opsForValue().get(BOOKING_SESSION_KEY_PREFIX + bookingSessionId);
        if (scheduleIdStr == null) {
            log.debug("[BookingSession] 이미 삭제된 세션 - sessionId={}", bookingSessionId);
            return false;
        }

        String userIdStr = redisTemplate.opsForValue().get(BOOKING_SESSION_USER_PREFIX + bookingSessionId);
        if (userIdStr == null) {
            log.warn("[BookingSession] userId 조회 실패, 부분 삭제 - sessionId={}", bookingSessionId);
            deletePartialSession(bookingSessionId, scheduleIdStr);
            return true;
        }

        Long scheduleId = Long.parseLong(scheduleIdStr);
        Long userId = Long.parseLong(userIdStr);

        deleteBookingSession(bookingSessionId, userId, scheduleId);

        return true;
    }

    /**
     * BookingSession 완전 삭제 (파라미터 있을 때 - 조회 불필요)
     */
    private void deleteBookingSession(String bookingSessionId, Long userId, Long scheduleId) {
        // Active에서 제거
        redisTemplate.opsForZSet().remove("active:" + scheduleId, bookingSessionId);

        // 모든 Redis 키 삭제
        redisTemplate.delete(BOOKING_SESSION_KEY_PREFIX + bookingSessionId);
        redisTemplate.delete(BOOKING_SESSION_DEVICE_PREFIX + bookingSessionId);
        redisTemplate.delete(BOOKING_SESSION_USER_PREFIX + bookingSessionId);
        redisTemplate.delete(BOOKING_SESSION_KEY_PREFIX + userId + ":" + scheduleId);

        log.info("[BookingSession] 완전 삭제 - sessionId={}, userId={}, scheduleId={}",
                bookingSessionId, userId, scheduleId);
    }

    // userId 없는 경우 부분 삭제
    private void deletePartialSession(String bookingSessionId, String scheduleIdStr) {
        Long scheduleId = Long.parseLong(scheduleIdStr);

        redisTemplate.opsForZSet().remove("active:" + scheduleId, bookingSessionId);
        redisTemplate.delete(BOOKING_SESSION_KEY_PREFIX + bookingSessionId);
        redisTemplate.delete(BOOKING_SESSION_DEVICE_PREFIX + bookingSessionId);
        redisTemplate.delete("booking:session:user:" + bookingSessionId);

        log.warn("[BookingSession] 부분 삭제 완료 - sessionId={}, scheduleId={}",
                bookingSessionId, scheduleId);
    }
}
