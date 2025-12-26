package back.kalender.domain.booking.seatHold.event;

import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 좌석 HOLD/RELEASE 이벤트 리스너
 *
 * DB 트랜잭션 커밋 후 Redis 작업을 수행
 * - AFTER_COMMIT: DB 커밋 성공 후에만 실행
 * - DB 롤백 시에는 실행되지 않음
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldEventListener {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String SEAT_HOLD_OWNER_KEY = "seat:hold:owner:%d:%d";
    private static final String SEAT_VERSION_KEY = "seat:version:%d";
    private static final String SEAT_CHANGES_KEY = "seat:changes:%d:%d";
    private static final long CHANGES_TTL_SECONDS = 60;

    // 좌석 홀드 완료 후 redis 작업
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSeatHoldCompleted(SeatHoldCompletedEvent event){
        try{
            // redis에 HOLD Owner 기록
            String holdOwnerKey = String.format(SEAT_HOLD_OWNER_KEY, event.getScheduleId(), event.getSeatId());
            redisTemplate.opsForValue().set(holdOwnerKey, event.getUserId().toString(), event.getHoldTtlSeconds(), TimeUnit.SECONDS);
            log.debug("[SeatHoldEvent] Redis owner 기록 완료 - key={}, userId={}, ttl={}s",
                    holdOwnerKey, event.getUserId(), event.getHoldTtlSeconds());

            // 변경 이벤트 발행(폴링용)
            recordSeatChangeEvent(event.getScheduleId(), event.getSeatId(), event.getStatus(), event.getUserId());
        } catch (Exception e) {;
            log.error("[SeatHoldEvent] Redis 작업 실패 (DB는 이미 커밋됨) - event={}",
                    event, e);
            handleRedisFailure(event);
        }
    }

    // 좌석 홀드 해제 완료 후 redis 작업
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSeatReleaseCompleted(SeatReleaseCompletedEvent event){
        try{
            // Redis HOLD Owner 키 삭제
            String holdOwnerKey = String.format(SEAT_HOLD_OWNER_KEY, event.getScheduleId(), event.getSeatId());

            Boolean deleted = redisTemplate.delete(holdOwnerKey);

            if(Boolean.FALSE.equals(deleted)){
                log.warn("[SeatReleaseEvent] Redis 키 삭제 실패 (키 없음) - key={}", holdOwnerKey);
            }

            // 변경 이벤트 발행(폴링용)
            recordSeatChangeEvent(event.getScheduleId(), event.getSeatId(), event.getStatus(), event.getUserId());
        }catch (Exception e){
            log.error("[SeatReleaseEvent] Redis 작업 실패 (DB는 이미 커밋됨) - event={}",
                    event, e);
        }
    }

    // 좌석 변경 이벤트 기록
    private void recordSeatChangeEvent(
            Long scheduleId,
            Long seatId,
            SeatStatus status,
            Long userId
    ) {
        try {
            // 버전 증가
            String versionKey = String.format(SEAT_VERSION_KEY, scheduleId);
            Long version = redisTemplate.opsForValue().increment(versionKey);

            // 이벤트 JSON 생성
            Map<String, Object> event = new HashMap<>();
            event.put("seatId", seatId);
            event.put("status", status.name());
            event.put("userId", userId);
            event.put("version", version);
            event.put("timestamp", LocalDateTime.now().toString());

            // Redis 저장 (TTL 60초)
            String changeKey = String.format(SEAT_CHANGES_KEY, scheduleId, version);
            String eventJson = objectMapper.writeValueAsString(event);

            redisTemplate.opsForValue().set(
                    changeKey,
                    eventJson,
                    CHANGES_TTL_SECONDS,
                    TimeUnit.SECONDS
            );

            log.debug("[SeatChangeEvent] 이벤트 발행 - version={}, seatId={}, status={}",
                    version, seatId, status);

        } catch (JsonProcessingException e) {
            log.error("[SeatChangeEvent] JSON 변환 실패 - seatId={}", seatId, e);
        } catch (Exception e) {
            log.error("[SeatChangeEvent] 이벤트 발행 실패 - seatId={}", seatId, e);
        }
    }

    /**
     * TODO: Redis 실패 시 보상 로직
     * - 재시도 큐에 추가 (RabbitMQ)
     */
    private void handleRedisFailure(SeatHoldCompletedEvent event) {
        // TODO: 보상 트랜잭션 구현
        log.error("[SeatHoldEvent] Redis 실패 보상 필요 - scheduleId={}, seatId={}, userId={}",
                event.getScheduleId(), event.getSeatId(), event.getUserId());

        // 실패 이벤트를 별도 큐에 저장
        // failureQueueService.enqueue(event);
    }
}
