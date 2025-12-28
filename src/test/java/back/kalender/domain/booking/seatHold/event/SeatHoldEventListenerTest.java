package back.kalender.domain.booking.seatHold.event;


import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatHoldEventListener 테스트")
public class SeatHoldEventListenerTest {
    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SeatHoldEventListener eventListener;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("HOLD 이벤트 수신 시 Redis에 owner 기록")
    void handleSeatHoldCompleted_Success() throws JsonProcessingException {
        Long scheduleId = 1L;
        Long seatId = 101L;
        Long userId = 1000L;
        Long ttl = 300L;

        SeatHoldCompletedEvent event = new SeatHoldCompletedEvent(
                scheduleId,
                seatId,
                userId,
                SeatStatus.HOLD,
                ttl
        );

        when(objectMapper.writeValueAsString(any(Map.class)))
                .thenReturn("{\"seatId\":101}");

        when(valueOperations.increment(anyString())).thenReturn(1L);

        eventListener.handleSeatHoldCompleted(event);

        verify(valueOperations, times(2)).set(
                anyString(),
                anyString(),
                anyLong(),
                any(TimeUnit.class)
        );

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> unitCaptor = ArgumentCaptor.forClass(TimeUnit.class);

        verify(valueOperations, times(2)).set(
                keyCaptor.capture(),
                valueCaptor.capture(),
                ttlCaptor.capture(),
                unitCaptor.capture()
        );

        // HOLD owner 기록
        assertThat(keyCaptor.getAllValues().get(0)).isEqualTo("seat:hold:owner:1:101");
        assertThat(valueCaptor.getAllValues().get(0)).isEqualTo("1000");
        assertThat(ttlCaptor.getAllValues().get(0)).isEqualTo(300L);
        assertThat(unitCaptor.getAllValues().get(0)).isEqualTo(TimeUnit.SECONDS);

        // seat change event 기록
        assertThat(keyCaptor.getAllValues().get(1)).isEqualTo("seat:changes:1:1");
        assertThat(ttlCaptor.getAllValues().get(1)).isEqualTo(60L);
        assertThat(unitCaptor.getAllValues().get(1)).isEqualTo(TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("HOLD 이벤트 처리 실패 시 예외를 잡아서 처리")
    void handleSeatHoldCompleted_RedisFailure() {
        SeatHoldCompletedEvent event = new SeatHoldCompletedEvent(
                1L, 101L, 1000L, SeatStatus.HOLD, 300L
        );

        doThrow(new RuntimeException("Redis connection failed"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // 예외를 잡아서 처리하므로 테스트는 성공해야 함
        eventListener.handleSeatHoldCompleted(event);

        verify(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("RELEASE 이벤트 수신 시 Redis owner 삭제")
    void handleSeatReleaseCompleted_Success() {
        Long scheduleId = 1L;
        Long seatId = 101L;
        Long userId = 1000L;

        SeatReleaseCompletedEvent event = new SeatReleaseCompletedEvent(
                scheduleId,
                seatId,
                userId,
                SeatStatus.AVAILABLE
        );

        when(redisTemplate.delete(anyString())).thenReturn(true);

        eventListener.handleSeatReleaseCompleted(event);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).delete(keyCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo("seat:hold:owner:1:101");
    }

    @Test
    @DisplayName("recordSeatChangeEvent - version 증가 및 이벤트 저장")
    void recordSeatChangeEvent_Success() throws Exception {
        Long scheduleId = 1L;
        Long seatId = 101L;
        Long userId = 1000L;
        Long expectedVersion = 5L;

        when(valueOperations.increment("seat:version:1")).thenReturn(expectedVersion);
        when(objectMapper.writeValueAsString(any(Map.class)))
                .thenReturn("{\"seatId\":101,\"status\":\"HOLD\"}");

        SeatHoldCompletedEvent event = new SeatHoldCompletedEvent(
                scheduleId, seatId, userId, SeatStatus.HOLD, 300L
        );
        eventListener.handleSeatHoldCompleted(event);

        // version 증가 검증
        verify(valueOperations).increment("seat:version:1");

        // change event 저장 검증
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(valueOperations, atLeastOnce()).set(
                keyCaptor.capture(),
                valueCaptor.capture(),
                eq(60L),
                eq(TimeUnit.SECONDS)
        );

        // change event 키 확인
        boolean hasChangeKey = keyCaptor.getAllValues().stream()
                .anyMatch(key -> key.equals("seat:changes:1:" + expectedVersion));
        assertThat(hasChangeKey).isTrue();
    }

    @Test
    @DisplayName("JSON 변환 실패 시에도 예외가 전파되지 않음")
    void recordSeatChangeEvent_JsonProcessingException() throws JsonProcessingException {
        SeatHoldCompletedEvent event = new SeatHoldCompletedEvent(
                1L, 101L, 1000L, SeatStatus.HOLD, 300L
        );

        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(objectMapper.writeValueAsString(any(Map.class)))
                .thenThrow(new JsonProcessingException("Test exception") {});

        eventListener.handleSeatHoldCompleted(event);

        // HOLD owner는 정상 기록되어야 함
        verify(valueOperations).set(
                eq("seat:hold:owner:1:101"),
                eq("1000"),
                eq(300L),
                eq(TimeUnit.SECONDS)
        );
    }
}
