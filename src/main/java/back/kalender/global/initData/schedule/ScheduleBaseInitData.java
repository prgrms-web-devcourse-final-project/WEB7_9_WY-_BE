package back.kalender.global.initData.schedule;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.artist.repository.ArtistRepository;
import back.kalender.domain.schedule.entity.Schedule;
import back.kalender.domain.schedule.enums.ScheduleCategory;
import back.kalender.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile({"prod", "dev"})
@Order(0)
@RequiredArgsConstructor
public class ScheduleBaseInitData implements ApplicationRunner {

    private final ArtistRepository artistRepository;
    private final ScheduleRepository scheduleRepository;

    @Override
    public void run(ApplicationArguments args) {

        for (Artist artist : artistRepository.findAll()) {

            // 아티스트 단위 중복 방지
            if (scheduleRepository.existsByArtistId(artist.getId())) {
                continue;
            }

            createSchedule(artist, ScheduleCategory.BROADCAST, 5, 20);
            createSchedule(artist, ScheduleCategory.FAN_SIGN, 10, 30);
            createSchedule(artist, ScheduleCategory.AWARD_SHOW, 15, 40);
            createSchedule(artist, ScheduleCategory.ANNIVERSARY, 1, 60);
        }
    }

    private void createSchedule(
            Artist artist,
            ScheduleCategory category,
            int minDay,
            int maxDay
    ) {
        scheduleRepository.save(
            Schedule.builder()
                .artistId(artist.getId())
                .scheduleCategory(category)
                .title(artist.getName() + " " + category.name())
                .scheduleTime(randomBetweenDecJan(minDay, maxDay))
                .location("온라인 / 방송국")
                .build()
        );
    }

    private LocalDateTime randomBetweenDecJan(int min, int max) {
        return LocalDateTime.of(2025, 12, 1, 18, 0)
            .plusDays(min + (int)(Math.random() * (max - min)));
    }
}