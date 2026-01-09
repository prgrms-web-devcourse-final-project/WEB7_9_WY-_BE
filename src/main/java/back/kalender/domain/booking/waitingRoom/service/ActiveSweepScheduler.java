package back.kalender.domain.booking.waitingRoom.service;

import back.kalender.domain.booking.reservation.service.ReservationService;
import back.kalender.domain.booking.session.service.BookingSessionService;
import back.kalender.domain.performance.schedule.service.ScheduleQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveSweepScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final ScheduleQueryService scheduleQueryService;
    private final BookingSessionService bookingSessionService;
    private final ReservationService reservationService;

    @Scheduled(fixedDelay = 1000)
    public void sweep() {
        long cutoff = System.currentTimeMillis() - 30_000;

        for (Long scheduleId : scheduleQueryService.getOpenScheduleIds()) {
            try{
                sweepSchedule(scheduleId, cutoff);
            }catch (Exception e) {
                log.error("[ActiveSweep] 스케줄 정리 실패 - scheduleId={}", scheduleId, e);
            }
        }
    }

    private void sweepSchedule(Long scheduleId, long cutoff) {
        String activeKey = "active:" + scheduleId;

        // 만료된 세션 id 조회
        Set<String> expiredSessions = redisTemplate.opsForZSet().rangeByScore(activeKey, 0, cutoff);

        if (expiredSessions == null || expiredSessions.isEmpty()) {
            return;
        }

        log.info("[ActiveSweep] 비활성 세션 발견 - scheduleId={}, count={}",
                scheduleId, expiredSessions.size());

        // Active에서 제거
        Long removed = redisTemplate.opsForZSet()
                .removeRangeByScore(activeKey, 0, cutoff);

        for (String sessionId : expiredSessions) {
            try {
                String userIdStr = redisTemplate.opsForValue()
                        .get("booking:session:user:" + sessionId);

                if (userIdStr != null) {
                    Long userId = Long.parseLong(userIdStr);

                    // 예매 취소
                    reservationService.cancelActiveReservationIfExists(userId, scheduleId);
                }

                // BookingSession 삭제
                bookingSessionService.deleteBookingSessionBySessionId(sessionId);

            } catch (Exception e) {
                log.error("[ActiveSweep] 세션 정리 실패 - sessionId={}", sessionId, e);
            }
        }

        log.info("[ActiveSweep] 정리 완료 - scheduleId={}, activeRemoved={}, ", scheduleId, removed);

    }
}