package back.kalender.global.initData.schedule;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.artist.repository.ArtistRepository;
import back.kalender.domain.schedule.entity.Schedule;
import back.kalender.domain.schedule.enums.ScheduleCategory;
import back.kalender.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile({"prod", "dev"})
@Order(1)  // Artist(Order 0) 이후에 실행되도록 변경
@RequiredArgsConstructor
@Slf4j
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

            // 12월부터 1월까지 골고루 분포되도록 날짜 범위 조정
            createSchedule(artist, ScheduleCategory.BROADCAST, 0, 20);      // 12월 1일 ~ 12월 21일
            createSchedule(artist, ScheduleCategory.FAN_SIGN, 10, 40);     // 12월 11일 ~ 1월 10일
            createSchedule(artist, ScheduleCategory.AWARD_SHOW, 20, 50);   // 12월 21일 ~ 1월 20일
            createSchedule(artist, ScheduleCategory.ANNIVERSARY, 30, 62);  // 12월 31일 ~ 2월 1일
        }

        log.info("Schedule 초기 데이터 생성 완료");
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

    /**
     * 2025년 12월 1일부터 시작하여 minDay~maxDay 사이의 랜덤 날짜 생성
     * 12월 1일 (day 0) ~ 2월 1일 (day 62)까지 커버
     */
    private LocalDateTime randomBetweenDecJan(int min, int max) {
        LocalDateTime startDate = LocalDateTime.of(2025, 12, 1, 18, 0);
        int randomDays = min + (int)(Math.random() * (max - min));
        return startDate.plusDays(randomDays);
    }
}