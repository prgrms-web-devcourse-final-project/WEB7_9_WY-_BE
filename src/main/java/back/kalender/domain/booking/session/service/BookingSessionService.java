package back.kalender.domain.booking.session.service;

import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingSessionService {

    private final StringRedisTemplate redisTemplate;
    private static final String BOOKING_SESSION_KEY_PREFIX = "booking:session:";
    private static final Duration BOOKING_SESSION_TTL = Duration.ofMinutes(30);

    /**
     * 대기열 통과 후 예매창 진입 시 호출
     * - bookingSessionId 발급
     * - value에는 scheduleId만 저장
     */
    public String create(Long userId, Long scheduleId) {
        String bookingSessionId = UUID.randomUUID().toString();

        String key = BOOKING_SESSION_KEY_PREFIX + bookingSessionId;
        redisTemplate.opsForValue().set(
                key,
                scheduleId.toString(),
                BOOKING_SESSION_TTL
        );

        return bookingSessionId;
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
    public void expire(String bookingSessionId) {
        redisTemplate.delete(BOOKING_SESSION_KEY_PREFIX + bookingSessionId);
    }


}
