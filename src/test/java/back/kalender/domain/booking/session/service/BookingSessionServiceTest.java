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

import java.time.Duration;

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

    private static final Long USER_ID = 1L;
    private static final Long SCHEDULE_ID = 10L;

    @BeforeEach
    void setUp() {
        bookingSessionService = new BookingSessionService(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
    }

    @Nested
    @DisplayName("create 테스트")
    class CreateTest {

        @Test
        @DisplayName("성공: 동일 userId + scheduleId로 이미 세션이 있으면 기존 세션을 재사용")
        void create_reuseExisting() {
            // given
            Long userId = 1L;
            Long scheduleId = 10L;
            String existingSessionId = "existing-session-id";

            // indexKey = booking:session:{userId}:{scheduleId}
            String indexKey = "booking:session:" + userId + ":" + scheduleId;

            // bookingSessionService.create() 내부에서
            // 1. indexKey 조회
            given(valueOps.get(indexKey)).willReturn(existingSessionId);

            // when
            String result = bookingSessionService.create(userId, scheduleId);

            // then
            // 기존 세션 ID를 그대로 반환
            assertThat(result).isEqualTo(existingSessionId);

            // 새 세션 생성 로직은 호출되지 않아야 함
            verify(valueOps, never())
                    .set(startsWith("booking:session:"), anyString(), any());

            // indexKey 재저장은 하지 않음 (재사용이므로)
            verify(valueOps, never())
                    .set(eq(indexKey), anyString(), any());
        }

        @Test
        @DisplayName("성공: 신규 세션 생성 시 sessionKey + indexKey 저장")
        void create_newSession() {
            // given
            given(valueOps.get(anyString())).willReturn(null);

            // when
            String sessionId = bookingSessionService.create(USER_ID, SCHEDULE_ID);

            // then
            assertThat(sessionId).isNotBlank();

            // sessionKey 저장 검증
            verify(valueOps).set(
                    eq("booking:session:" + sessionId),
                    eq(SCHEDULE_ID.toString()),
                    any(Duration.class)
            );

            // indexKey 저장 검증
            verify(valueOps).set(
                    eq("booking:session:" + USER_ID + ":" + SCHEDULE_ID),
                    eq(sessionId),
                    any(Duration.class)
            );
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
    }
}

//    TODO: c파트 연동 후 작성 예정
//    @Nested
//    @DisplayName("expire 테스트")
//    class ExpireTest {
//
//        @Test
//        @DisplayName("성공: booking session 삭제")
//        void expire_success() {
//            // given
//            String sessionId = "sid";
//
//            // when
//            bookingSessionService.expire(sessionId);
//
//            // then
//            verify(redisTemplate, times(1)).delete("booking:session:" + sessionId);
//        }
//    }
// }