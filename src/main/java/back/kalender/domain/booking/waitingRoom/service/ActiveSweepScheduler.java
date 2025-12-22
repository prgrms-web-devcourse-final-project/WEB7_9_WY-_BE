package back.kalender.domain.booking.waitingRoom.service;

import back.kalender.domain.performance.schedule.service.ScheduleQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveSweepScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final ScheduleQueryService scheduleQueryService;

    @Scheduled(fixedDelay = 5000)
    public void sweep() {
        long cutoff = System.currentTimeMillis() - 60_000;

        for (Long scheduleId : scheduleQueryService.getOpenScheduleIds()) {
            redisTemplate.opsForZSet()
                    .removeRangeByScore(
                            "active:" + scheduleId,
                            0,
                            cutoff
                    );
        }
    }
}