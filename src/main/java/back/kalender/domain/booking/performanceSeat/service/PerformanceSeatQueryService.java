package back.kalender.domain.booking.performanceSeat.service;

import back.kalender.domain.booking.performanceSeat.dto.PerformanceSeatResponse;
import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.booking.waitingRoom.service.QueueAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceSeatQueryService {

    private final PerformanceSeatRepository performanceSeatRepository;
    private final QueueAccessService queueAccessService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String SEAT_SOLD_SET_KEY = "seat:sold:%d";
    private static final String SEAT_HOLD_OWNER_KEY = "seat:hold:owner:%d:%d";

//    @Cacheable(
//            cacheNames = "seatLayout",
//            key = "#scheduleId"
//    )
    @Transactional(readOnly = true)
    public List<PerformanceSeatResponse> getSeatsByScheduleId(
        Long scheduleId,
        String bookingSessionId
    ) {
        // 1. Active 상태 체크
        queueAccessService.checkSeatAccess(scheduleId, bookingSessionId);

        // 2. Redis SOLD set 조회
        String soldSetKey = String.format(SEAT_SOLD_SET_KEY, scheduleId);
        Set<String> soldSeatIdsStr = redisTemplate.opsForSet().members(soldSetKey);

        Set<Long> soldSeatIds = soldSeatIdsStr == null ? Set.of()
                : soldSeatIdsStr.stream()
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        log.debug("[SeatQuery] Redis SOLD 좌석 수 - scheduleId={}, count={}",
                scheduleId, soldSeatIds.size());

        // 3. Redis HOLD owner 키 패턴 조회
        String holdPattern = String.format("seat:hold:owner:%d:*", scheduleId);
        Set<String> holdKeys = redisTemplate.keys(holdPattern);

        Set<Long> heldSeatIds = holdKeys == null ? Set.of()
                : holdKeys.stream()
                .map(key -> {
                    String[] parts = key.split(":");
                    return Long.parseLong(parts[4]); // seat:hold:owner:{scheduleId}:{seatId}
                })
                .collect(Collectors.toSet());

        log.debug("[SeatQuery] Redis HOLD 좌석 수 - scheduleId={}, count={}",
                scheduleId, heldSeatIds.size());

        // 4. DB 조회 + Redis 상태 우선 적용
        return performanceSeatRepository.findAllByScheduleId(scheduleId)
                .stream()
                .map(seat -> {
                    SeatStatus finalStatus;

                    // Redis 우선 체크 (Redis가 Source of Truth)
                    if (soldSeatIds.contains(seat.getId())) {
                        finalStatus = SeatStatus.SOLD;

                        // DB와 다르면 로그 (디버깅용)
                        if (seat.getStatus() != SeatStatus.SOLD) {
                            log.warn("[SeatQuery] DB-Redis 불일치(SOLD) - seatId={}, DB={}, Redis=SOLD",
                                    seat.getId(), seat.getStatus());
                        }

                    } else if (heldSeatIds.contains(seat.getId())) {
                        finalStatus = SeatStatus.HOLD;

                        // DB와 다르면 로그
                        if (seat.getStatus() != SeatStatus.HOLD) {
                            log.warn("[SeatQuery] DB-Redis 불일치(HOLD) - seatId={}, DB={}, Redis=HOLD",
                                    seat.getId(), seat.getStatus());
                        }

                    } else {
                        // Redis에 없으면 DB 상태 사용
                        finalStatus = seat.getStatus();
                    }

                    return PerformanceSeatResponse.from(seat, finalStatus);
                })
                .toList();
    }
}
