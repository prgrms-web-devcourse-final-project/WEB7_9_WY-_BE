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

    private static final Long USER_ID = 1L;
    private static final Long SCHEDULE_ID = 10L;

    @BeforeEach
    void setUp() {
        bookingSessionService = new BookingSessionService(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
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
            // 1. waitingToken 검증
            String waitingKey = "waiting:" + WAITING_TOKEN;
            given(valueOps.get(waitingKey))
                    .willReturn(QSID + ":" + SCHEDULE_ID);

            // 2. qsid로 deviceId 검증
            String qsidKey = "qsid:" + QSID;
            given(valueOps.get(qsidKey))
                    .willReturn(DEVICE_ID + ":" + SCHEDULE_ID);

            // 3. 기존 세션 없음
            String mappingKey = "booking:session:" + USER_ID + ":" + SCHEDULE_ID;
            given(valueOps.get(mappingKey)).willReturn(null);

            // 4. Active 추가 (ZSet)
            given(zSetOps.add(anyString(), anyString(), anyDouble()))
                    .willReturn(true);

            // when
            String bookingSessionId = bookingSessionService.createWithWaitingToken(
                    USER_ID, SCHEDULE_ID, WAITING_TOKEN, DEVICE_ID
            );

            // then
            assertThat(bookingSessionId).isNotBlank();

            // 1. 세션 데이터 저장
            verify(valueOps).set(
                    eq("booking:session:" + bookingSessionId),
                    eq(SCHEDULE_ID.toString()),
                    any(Duration.class)
            );

            // 2. deviceId 매핑 저장
            verify(valueOps).set(
                    eq("booking:session:device:" + bookingSessionId),
                    eq(DEVICE_ID),
                    any(Duration.class)
            );

            // 3. userId:scheduleId 매핑 저장
            verify(valueOps).set(
                    eq("booking:session:" + USER_ID + ":" + SCHEDULE_ID),
                    eq(bookingSessionId),
                    any(Duration.class)
            );

            // 4. Active 추가
            verify(zSetOps).add(
                    eq("active:" + SCHEDULE_ID),
                    eq(bookingSessionId),
                    anyDouble()
            );

            // 5. waitingToken 소비
            verify(redisTemplate).delete("waiting:" + WAITING_TOKEN);
        }

        @Test
        @DisplayName("실패: waitingToken이 유효하지 않음")
        void createWithWaitingToken_Fail_InvalidWaitingToken() {
            // given
            String waitingKey = "waiting:" + WAITING_TOKEN;
            given(valueOps.get(waitingKey)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> bookingSessionService.createWithWaitingToken(
                    USER_ID, SCHEDULE_ID, WAITING_TOKEN, DEVICE_ID
            ))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_WAITING_TOKEN);
        }

        @Test
        @DisplayName("실패: scheduleId 불일치")
        void createWithWaitingToken_Fail_ScheduleMismatch() {
            // given
            String waitingKey = "waiting:" + WAITING_TOKEN;
            Long wrongScheduleId = 999L;
            given(valueOps.get(waitingKey))
                    .willReturn(QSID + ":" + wrongScheduleId);

            // when & then
            assertThatThrownBy(() -> bookingSessionService.createWithWaitingToken(
                    USER_ID, SCHEDULE_ID, WAITING_TOKEN, DEVICE_ID
            ))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SCHEDULE_MISMATCH);
        }

        @Test
        @DisplayName("실패: qsid 만료됨")
        void createWithWaitingToken_Fail_QsidExpired() {
            // given
            String waitingKey = "waiting:" + WAITING_TOKEN;
            given(valueOps.get(waitingKey))
                    .willReturn(QSID + ":" + SCHEDULE_ID);

            String qsidKey = "qsid:" + QSID;
            given(valueOps.get(qsidKey)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> bookingSessionService.createWithWaitingToken(
                    USER_ID, SCHEDULE_ID, WAITING_TOKEN, DEVICE_ID
            ))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.QSID_EXPIRED);
        }

        @Test
        @DisplayName("실패: deviceId 불일치")
        void createWithWaitingToken_Fail_DeviceMismatch() {
            // given
            String waitingKey = "waiting:" + WAITING_TOKEN;
            given(valueOps.get(waitingKey))
                    .willReturn(QSID + ":" + SCHEDULE_ID);

            String qsidKey = "qsid:" + QSID;
            String originalDeviceId = "device-original";
            given(valueOps.get(qsidKey))
                    .willReturn(originalDeviceId + ":" + SCHEDULE_ID);

            // when & then
            assertThatThrownBy(() -> bookingSessionService.createWithWaitingToken(
                    USER_ID, SCHEDULE_ID, WAITING_TOKEN, DEVICE_ID
            ))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DEVICE_ID_MISMATCH);
        }

        @Test
        @DisplayName("성공: 기존 세션 재사용 (동일 기기)")
        void createWithWaitingToken_ReuseExistingSession() {
            // given
            String existingSessionId = "existing-session-123";

            // waitingToken 검증
            String waitingKey = "waiting:" + WAITING_TOKEN;
            given(valueOps.get(waitingKey))
                    .willReturn(QSID + ":" + SCHEDULE_ID);

            // deviceId 검증
            String qsidKey = "qsid:" + QSID;
            given(valueOps.get(qsidKey))
                    .willReturn(DEVICE_ID + ":" + SCHEDULE_ID);

            // 기존 세션 존재
            String mappingKey = "booking:session:" + USER_ID + ":" + SCHEDULE_ID;
            given(valueOps.get(mappingKey)).willReturn(existingSessionId);

            // 기존 세션의 deviceId 확인
            String sessionDeviceKey = "booking:session:device:" + existingSessionId;
            given(valueOps.get(sessionDeviceKey)).willReturn(DEVICE_ID);

            // when
            String result = bookingSessionService.createWithWaitingToken(
                    USER_ID, SCHEDULE_ID, WAITING_TOKEN, DEVICE_ID
            );

            // then
            assertThat(result).isEqualTo(existingSessionId);

            // 새 세션 생성 안 함
            verify(valueOps, never()).set(
                    startsWith("booking:session:"),
                    anyString(),
                    anyLong(),
                    any(TimeUnit.class)
            );

            // Active 추가 안 함 (이미 있으므로)
            verify(zSetOps, never()).add(anyString(), anyString(), anyDouble());

            // waitingToken 소비
            verify(redisTemplate).delete("waiting:" + WAITING_TOKEN);
        }

        @Test
        @DisplayName("실패: 다른 기기로 이미 세션 생성됨")
        void createWithWaitingToken_Fail_DeviceAlreadyUsed() {
            // given
            String existingSessionId = "existing-session-123";
            String otherDeviceId = "device-other";

            String waitingKey = "waiting:" + WAITING_TOKEN;
            given(valueOps.get(waitingKey))
                    .willReturn(QSID + ":" + SCHEDULE_ID);

            String qsidKey = "qsid:" + QSID;
            given(valueOps.get(qsidKey))
                    .willReturn(DEVICE_ID + ":" + SCHEDULE_ID);

            String mappingKey = "booking:session:" + USER_ID + ":" + SCHEDULE_ID;
            given(valueOps.get(mappingKey)).willReturn(existingSessionId);

            String sessionDeviceKey = "booking:session:device:" + existingSessionId;
            given(valueOps.get(sessionDeviceKey)).willReturn(otherDeviceId); // 다른 기기!

            // when & then
            assertThatThrownBy(() -> bookingSessionService.createWithWaitingToken(
                    USER_ID, SCHEDULE_ID, WAITING_TOKEN, DEVICE_ID
            ))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DEVICE_ALREADY_USED);
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
                    .willReturn(false); // 이미 있으므로 업데이트

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
                    .willReturn(null); // Active에 없음

            // when & then
            assertThatThrownBy(() -> bookingSessionService.ping(SCHEDULE_ID, BOOKING_SESSION_ID))
                    .isInstanceOf(ServiceException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_IN_ACTIVE);

            // score 갱신 안 함
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
                    .willReturn(1L); // 1개 제거됨

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
                    .willReturn(0L); // 없었음

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
                String key = "booking:session:" + sessionId;
                given(valueOps.get(key)).willReturn(null);

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
                String key = "booking:session:" + sessionId;
                given(valueOps.get(key)).willReturn(SCHEDULE_ID.toString());

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
                String key = "booking:session:" + sessionId;
                given(valueOps.get(key)).willReturn(null);

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
                String key = "booking:session:" + sessionId;
                given(valueOps.get(key)).willReturn("999");

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
                String key = "booking:session:" + sessionId;
                given(valueOps.get(key)).willReturn(SCHEDULE_ID.toString());

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
            // Active에서 제거
            verify(zSetOps).remove("active:" + scheduleId, bookingSessionId);

            // 4개 키 모두 삭제
            verify(redisTemplate).delete("booking:session:" + bookingSessionId);
            verify(redisTemplate).delete("booking:session:device:" + bookingSessionId);
            verify(redisTemplate).delete("booking:session:user:" + bookingSessionId);
            verify(redisTemplate).delete("booking:session:" + userId + ":" + scheduleId);

            // Redis 조회 안 함 (파라미터 활용)
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
            Long userId = 1L;
            Long scheduleId = 10L;

            // scheduleId 조회
            given(valueOps.get("booking:session:" + bookingSessionId))
                    .willReturn(scheduleId.toString());

            // userId 조회 (역매핑)
            given(valueOps.get("booking:session:user:" + bookingSessionId))
                    .willReturn(userId.toString());

            // when
            boolean result = bookingSessionService.deleteBookingSessionBySessionId(bookingSessionId);

            // then
            assertThat(result).isTrue();

            // Redis 조회 2회 (scheduleId, userId)
            verify(valueOps).get("booking:session:" + bookingSessionId);
            verify(valueOps).get("booking:session:user:" + bookingSessionId);

            // Active 제거
            verify(zSetOps).remove("active:" + scheduleId, bookingSessionId);

            // 4개 키 삭제
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

            // 삭제 시도 안 함
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
                    .willReturn(null); // userId 없음!

            // when
            boolean result = bookingSessionService.deleteBookingSessionBySessionId(bookingSessionId);

            // then
            assertThat(result).isTrue();

            // Active 제거
            verify(zSetOps).remove("active:" + scheduleId, bookingSessionId);

            // 부분 삭제 (3개 키만)
            verify(redisTemplate, times(3)).delete(anyString());
        }
    }

    @Nested
    @DisplayName("checkExistingSession 테스트 (Active 기반)")
    class CheckExistingSessionTest {

        @Test
        @DisplayName("성공: Active에 있는 세션 재사용")
        void checkExistingSession_ActiveSession_Reuse() {
            // given
            String existingSessionId = "bs_existing";
            Long userId = 1L;
            Long scheduleId = 10L;
            String deviceId = "device-123";

            // mapping 조회
            given(valueOps.get("booking:session:" + userId + ":" + scheduleId))
                    .willReturn(existingSessionId);

            // Active 확인 (있음)
            given(zSetOps.score("active:" + scheduleId, existingSessionId))
                    .willReturn(1000.0);

            // deviceId 확인
            given(valueOps.get("booking:session:device:" + existingSessionId))
                    .willReturn(deviceId);

            // when
            // createWithWaitingToken 내부에서 checkExistingSession 호출
            String waitingToken = "wt_test";
            String qsid = "qsid_test";

            given(valueOps.get("waiting:" + waitingToken))
                    .willReturn(qsid + ":" + scheduleId);
            given(valueOps.get("qsid:" + qsid))
                    .willReturn(deviceId + ":" + scheduleId);

            String result = bookingSessionService.createWithWaitingToken(
                    userId, scheduleId, waitingToken, deviceId
            );

            // then
            assertThat(result).isEqualTo(existingSessionId);

            // 새 세션 생성 안 함
            verify(valueOps, never()).set(
                    startsWith("booking:session:"),
                    anyString(),
                    any(Duration.class)
            );
        }

        @Test
        @DisplayName("성공: Active에 없는 세션 삭제 후 새로 생성")
        void checkExistingSession_InactiveSession_DeleteAndCreate() {
            // given
            String oldSessionId = "bs_old";
            Long userId = 1L;
            Long scheduleId = 10L;
            String deviceId = "device-123";

            // mapping 조회 (기존 세션 발견)
            given(valueOps.get("booking:session:" + userId + ":" + scheduleId))
                    .willReturn(oldSessionId);

            // Active 확인 (없음!)
            given(zSetOps.score("active:" + scheduleId, oldSessionId))
                    .willReturn(null);

            // deleteBookingSessionBySessionId 호출 시 필요한 mock
            given(valueOps.get("booking:session:" + oldSessionId))
                    .willReturn(scheduleId.toString());
            given(valueOps.get("booking:session:user:" + oldSessionId))
                    .willReturn(userId.toString());

            // createWithWaitingToken 관련 mock
            String waitingToken = "wt_test";
            String qsid = "qsid_test";
            given(valueOps.get("waiting:" + waitingToken))
                    .willReturn(qsid + ":" + scheduleId);
            given(valueOps.get("qsid:" + qsid))
                    .willReturn(deviceId + ":" + scheduleId);

            // when
            String result = bookingSessionService.createWithWaitingToken(
                    userId, scheduleId, waitingToken, deviceId
            );

            // then
            assertThat(result).isNotEqualTo(oldSessionId); // 새 세션 생성됨

            // 기존 세션 삭제됨
            verify(redisTemplate, atLeastOnce()).delete(
                    "booking:session:" + oldSessionId
            );

            // 새 세션 생성됨
            verify(valueOps, atLeastOnce()).set(
                    startsWith("booking:session:"),
                    anyString(),
                    any(Duration.class)
            );
        }
    }
}

