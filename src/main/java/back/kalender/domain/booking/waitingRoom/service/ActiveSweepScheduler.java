package back.kalender.domain.booking.waitingRoom.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveSweepScheduler {

    private final RedisTemplate<String, String> redisTemplate;

    @Scheduled(fixedDelay = 5000)
    public void sweep() {
        long cutoff = System.currentTimeMillis() - 60_000; // 60초
        redisTemplate.opsForZSet()
                .removeRangeByScore("active:1", 0, cutoff);
    }
}