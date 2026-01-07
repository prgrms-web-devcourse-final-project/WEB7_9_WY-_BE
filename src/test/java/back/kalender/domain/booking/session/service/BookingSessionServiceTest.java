package back.kalender.domain.booking.session.service;

import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingSessionService 단위 테스트")
class BookingSessionServiceTest {

    private BookingSessionService bookingSessionService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private ZSetOperations<String, String> zSetOps;

    @Mock
    private HashOperations<String, Object, Object> hashOps;

    private static final Long USER_ID = 1L;
    private static final Long SCHEDULE_ID = 10L;

    @BeforeEach
    void setUp() {
        bookingSessionService = new BookingSessionService(redisTemplate);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOps);

        // ✅ waitingToken lock 통과시키기 (중복 요청 방지 락)
        // createWithWaitingToken()이 제일 먼저 setIfAbsent(lockKey, "1", 10, SECONDS)를 수행함
        lenient().when(valueOps.setIfAbsent(
                startsWith("waiting:lock:"),
                eq("1"),
                anyLong(),
                eq(TimeUnit.SECONDS)
        )).thenReturn(true);

        // zset add 기본 true
        lenient().when(zSetOps.add(anyString(), anyString(), anyDouble())).thenReturn(true);
    }

    @Nested
    @DisplayName("createWithWaitingToken 테스트")
    class CreateWithWaitingTokenTest {

        private static final String WAITING_TOKEN = "wt_abc123xyz";
        private static final String DEVICE_ID = "device-xxx";
        private static final String QSID = "qsid-123";

        @Test
        @DisplayName("성공: 신규 BookingSession 생성 + Active 추가")
        void createWithWaitingToken_Success() {
            // given
            given(valueOps.get("waiting:" + WAITING_TOKEN))
                    .willReturn(QSID + ":" + SCHEDULE_ID);

            given(valueOps.get("qsid:" + QSID))
                    .willReturn(DEVICE_ID + ":" + SCHEDULE_ID);

            // 기존 세션 없음
            given(valueOps.get("booking:session:" + USER_ID + ":" + SCHEDULE_ID))
                    .willReturn(null);

            // when
            String bookingSessionId = bookingSessionService.createWithWaitingToken(
                    USER_ID, SCHEDULE_ID, WAITING_TOKEN, DEVICE_ID
            );

            // then
            assertThat(bookingSessionId).isNotBlank();

            verify(valueOps).set(
                    eq("booking:session:" + bookingSessionId),
                    eq(SCHEDULE_ID.toString()),
                    any(Duration.class)
            );

            verify(valueOps).set(
                    eq("booking:session:device:" + bookingSessionId),
                    eq(DEVICE_ID),
                    any(Duration.class)
            );

            verify(valueOps).set(
                    eq("booking:session:" + USER_ID + ":" + SCHEDULE_ID),
                    eq(bookingSessionId),
                    any(Duration.class)
            );

            verify(zSetOps).add(
                    eq("active:" + SCHEDULE_ID),
                    eq(bookingSessionId),
                    anyDouble()
            );

            // waitingToken 소비
            verify(redisTemplate).delete("waiting:" + WAITING_TOKEN);

            // ✅ admitted / qsid / device 정리
            verify(hashOps).delete("admitted:" + SCHEDULE_ID, QSID);
            verify(redisTemplate).delete("qsid:" + QSID);
            verify(redisTemplate).delete("device:" + SCHEDULE_ID + ":" + DEVICE_ID);

            // ✅ lock 해제도 수행됨(성공/실패 관계없이 finally)
            verify(redisTemplate).delete("waiting:lock:" + WAITING_TOKEN);
        }

        @Test
        @DisplayName("실패: waitingToken이 유효하지 않음")
        void createWithWaitingToken_Fail_InvalidWaitingToken() {
            // given
            given(valueOps.get("waiting:" + WAITING_TOKEN)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> bookingSessionService.createWithWaitingToken(
                    USER_ID, SCHEDULE_ID, WAITING_TOKEN, DEVICE_ID
            ))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_WAITING_TOKEN);

            verify(redisTemplate).delete("waiting:lock:" + WAITING_TOKEN);
        }

        @Test
        @DisplayName("실패: scheduleId 불일치")
        void createWithWaitingToken_Fail_ScheduleMismatch() {
            // given
            Long wrongScheduleId = 999L;
            given(valueOps.get("waiting:" + WAITING_TOKEN))
                    .willReturn(QSID + ":" + wrongScheduleId);

            // when & then
            assertThatThrownBy(() -> bookingSessionService.createWithWaitingToken(
                    USER_ID, SCHEDULE_ID, WAITING_TOKEN, DEVICE_ID
            ))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCHEDULE_MISMATCH);

            verify(redisTemplate).delete("waiting:lock:" + WAITING_TOKEN);
        }

        @Test
        @DisplayName("실패: qsid 만료됨")
        void createWithWaitingToken_Fail_QsidExpired() {
            // given
            given(valueOps.get("waiting:" + WAITING_TOKEN))
                    .willReturn(QSID + ":" + SCHEDULE_ID);

            given(valueOps.get("qsid:" + QSID)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> bookingSessionService.createWithWaitingToken(
                    USER_ID, SCHEDULE_ID, WAITING_TOKEN, DEVICE_ID
            ))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.QSID_EXPIRED);

            verify(redisTemplate).delete("waiting:lock:" + WAITING_TOKEN);
        }

        @Test
        @DisplayName("실패: deviceId 불일치")
        void createWithWaitingToken_Fail_DeviceMismatch() {
            // given
            given(valueOps.get("waiting:" + WAITING_TOKEN))
                    .willReturn(QSID + ":" + SCHEDULE_ID);

            String originalDeviceId = "device-original";
            given(valueOps.get("qsid:" + QSID))
                    .willReturn(originalDeviceId + ":" + SCHEDULE_ID);

            // when & then
            assertThatThrownBy(() -> bookingSessionService.createWithWaitingToken(
                    USER_ID, SCHEDULE_ID, WAITING_TOKEN, DEVICE_ID
            ))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DEVICE_ID_MISMATCH);

            verify(redisTemplate).delete("waiting:lock:" + WAITING_TOKEN);
        }

        @Test
        @DisplayName("성공: 기존 세션 발견 시 무조건 삭제 후 새로 생성")
        void createWithWaitingToken_AlwaysDeleteAndRecreate() {
            // given
            String existingSessionId = "existing-session-123";

            // waitingToken 검증
            given(valueOps.get("waiting:" + WAITING_TOKEN))
                    .willReturn(QSID + ":" + SCHEDULE_ID);

            // deviceId 검증
            given(valueOps.get("qsid:" + QSID))
                    .willReturn(DEVICE_ID + ":" + SCHEDULE_ID);

            // 기존 세션 존재
            String mappingKey = "booking:session:" + USER_ID + ":" + SCHEDULE_ID;
            given(valueOps.get(mappingKey))
                    .willReturn(existingSessionId);

            // deleteBookingSessionBySessionId 내부 조회용
            given(valueOps.get("booking:session:" + existingSessionId))
                    .willReturn(SCHEDULE_ID.toString());

            given(valueOps.get("booking:session:user:" + existingSessionId))
                    .willReturn(USER_ID.toString());

            // when
            String result = bookingSessionService.createWithWaitingToken(
                    USER_ID, SCHEDULE_ID, WAITING_TOKEN, DEVICE_ID
            );

            // then
            assertThat(result).isNotBlank();
            assertThat(result).isNotEqualTo(existingSessionId);

            // 기존 세션 삭제(완전 삭제 로직에서 여러 delete 발생)
            verify(zSetOps).remove("active:" + SCHEDULE_ID, existingSessionId);

            // waitingToken 소비 + lock 해제
            verify(redisTemplate).delete("waiting:" + WAITING_TOKEN);
            verify(redisTemplate).delete("waiting:lock:" + WAITING_TOKEN);
        }
    }

    @Nested
    @DisplayName("ping 테스트")
    class PingTest {
        private static final String BOOKING_SESSION_ID = "bs_abc123";

        @Test
        @DisplayName("성공: Active score 갱신")
        void ping_Success() {
            // given
            String activeKey = "active:" + SCHEDULE_ID;
            given(zSetOps.score(activeKey, BOOKING_SESSION_ID))
                    .willReturn(1000.0);

            given(zSetOps.add(eq(activeKey), eq(BOOKING_SESSION_ID), anyDouble()))
                    .willReturn(false);

            // when
            bookingSessionService.ping(SCHEDULE_ID, BOOKING_SESSION_ID);

            // then
            verify(zSetOps).score(activeKey, BOOKING_SESSION_ID);
            verify(zSetOps).add(eq(activeKey), eq(BOOKING_SESSION_ID), anyDouble());
        }

        @Test
        @DisplayName("실패: Active에 없음")
        void ping_Fail_NotInActive() {
            // given
            String activeKey = "active:" + SCHEDULE_ID;
            given(zSetOps.score(activeKey, BOOKING_SESSION_ID))
                    .willReturn(null);

            // when & then
            assertThatThrownBy(() -> bookingSessionService.ping(SCHEDULE_ID, BOOKING_SESSION_ID))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_IN_ACTIVE);

            verify(zSetOps, never()).add(anyString(), anyString(), anyDouble());
        }
    }

    @Nested
    @DisplayName("leaveActive 테스트")
    class LeaveActiveTest {
        private static final String BOOKING_SESSION_ID = "bs_abc123";

        @Test
        @DisplayName("성공: Active에서 제거")
        void leaveActive_Success() {
            // given
            String activeKey = "active:" + SCHEDULE_ID;
            given(zSetOps.remove(activeKey, BOOKING_SESSION_ID))
                    .willReturn(1L);

            // when
            bookingSessionService.leaveActive(SCHEDULE_ID, BOOKING_SESSION_ID);

            // then
            verify(zSetOps).remove(activeKey, BOOKING_SESSION_ID);
        }

        @Test
        @DisplayName("성공: Active에 없어도 예외 발생 안 함")
        void leaveActive_Success_NotInActive() {
            // given
            String activeKey = "active:" + SCHEDULE_ID;
            given(zSetOps.remove(activeKey, BOOKING_SESSION_ID))
                    .willReturn(0L);

            // when & then
            assertThatCode(() -> bookingSessionService.leaveActive(SCHEDULE_ID, BOOKING_SESSION_ID))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateExists 테스트")
    class ValidateExistsTest {

        @Test
        @DisplayName("실패: 세션 키가 없으면 BOOKING_SESSION_EXPIRED")
        void validateExists_expired() {
            // given
            String sessionId = "sid";
            given(valueOps.get("booking:session:" + sessionId)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> bookingSessionService.validateExists(sessionId))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.BOOKING_SESSION_EXPIRED);
        }

        @Test
        @DisplayName("성공: 세션 키가 있으면 통과")
        void validateExists_success() {
            // given
            String sessionId = "sid";
            given(valueOps.get("booking:session:" + sessionId)).willReturn(SCHEDULE_ID.toString());

            // when & then
            assertThatCode(() -> bookingSessionService.validateExists(sessionId))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateForSchedule 테스트")
    class ValidateForScheduleTest {

        @Test
        @DisplayName("실패: 세션이 없으면 BOOKING_SESSION_EXPIRED")
        void validateForSchedule_expired() {
            // given
            String sessionId = "sid";
            given(valueOps.get("booking:session:" + sessionId)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> bookingSessionService.validateForSchedule(sessionId, SCHEDULE_ID))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.BOOKING_SESSION_EXPIRED);
        }

        @Test
        @DisplayName("실패: scheduleId가 다르면 INVALID_BOOKING_SESSION")
        void validateForSchedule_mismatch() {
            // given
            String sessionId = "sid";
            given(valueOps.get("booking:session:" + sessionId)).willReturn("999");

            // when & then
            assertThatThrownBy(() -> bookingSessionService.validateForSchedule(sessionId, SCHEDULE_ID))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_BOOKING_SESSION);
        }

        @Test
        @DisplayName("성공: scheduleId가 일치하면 통과")
        void validateForSchedule_success() {
            // given
            String sessionId = "sid";
            given(valueOps.get("booking:session:" + sessionId)).willReturn(SCHEDULE_ID.toString());

            // when & then
            assertThatCode(() -> bookingSessionService.validateForSchedule(sessionId, SCHEDULE_ID))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("expire 테스트")
    class ExpireTest {

        @Test
        @DisplayName("성공: userId, scheduleId 파라미터 활용하여 삭제")
        void expire_Success_WithParameters() {
            // given
            String bookingSessionId = "bs_abc123";
            Long userId = 1L;
            Long scheduleId = 10L;

            // when
            bookingSessionService.expire(bookingSessionId, userId, scheduleId);

            // then
            verify(zSetOps).remove("active:" + scheduleId, bookingSessionId);

            verify(redisTemplate).delete("booking:session:" + bookingSessionId);
            verify(redisTemplate).delete("booking:session:device:" + bookingSessionId);
            verify(redisTemplate).delete("booking:session:user:" + bookingSessionId);
            verify(redisTemplate).delete("booking:session:" + userId + ":" + scheduleId);

            verify(valueOps, never()).get(anyString());
        }
    }

    @Nested
    @DisplayName("deleteBookingSessionBySessionId 테스트")
    class DeleteBookingSessionBySessionIdTest {

        @Test
        @DisplayName("성공: sessionId로 조회 후 삭제")
        void deleteBySessionId_Success() {
            // given
            String bookingSessionId = "bs_abc123";
            Long scheduleId = 10L;
            Long userId = 1L;

            given(valueOps.get("booking:session:" + bookingSessionId))
                    .willReturn(scheduleId.toString());

            given(valueOps.get("booking:session:user:" + bookingSessionId))
                    .willReturn(userId.toString());

            // when
            boolean result = bookingSessionService.deleteBookingSessionBySessionId(bookingSessionId);

            // then
            assertThat(result).isTrue();

            verify(valueOps).get("booking:session:" + bookingSessionId);
            verify(valueOps).get("booking:session:user:" + bookingSessionId);

            verify(zSetOps).remove("active:" + scheduleId, bookingSessionId);

            // deleteBookingSession()에서 4개 delete 호출
            verify(redisTemplate, times(4)).delete(anyString());
        }

        @Test
        @DisplayName("실패: 이미 삭제된 세션 (scheduleId 없음)")
        void deleteBySessionId_AlreadyDeleted() {
            // given
            String bookingSessionId = "bs_abc123";
            given(valueOps.get("booking:session:" + bookingSessionId))
                    .willReturn(null);

            // when
            boolean result = bookingSessionService.deleteBookingSessionBySessionId(bookingSessionId);

            // then
            assertThat(result).isFalse();

            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("경고: userId 조회 실패 시 부분 삭제")
        void deleteBySessionId_PartialDelete_WhenUserIdMissing() {
            // given
            String bookingSessionId = "bs_abc123";
            Long scheduleId = 10L;

            given(valueOps.get("booking:session:" + bookingSessionId))
                    .willReturn(scheduleId.toString());

            given(valueOps.get("booking:session:user:" + bookingSessionId))
                    .willReturn(null);

            // when
            boolean result = bookingSessionService.deleteBookingSessionBySessionId(bookingSessionId);

            // then
            assertThat(result).isTrue();

            verify(zSetOps).remove("active:" + scheduleId, bookingSessionId);

            // deletePartialSession()은 3개 delete
            verify(redisTemplate, times(3)).delete(anyString());
        }
    }
}
