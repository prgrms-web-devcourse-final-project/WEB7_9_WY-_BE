package back.kalender.global.initData.performance;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.artist.repository.ArtistRepository;
import back.kalender.domain.performance.performance.entity.Performance;
import back.kalender.domain.performance.performance.repository.PerformanceRepository;
import back.kalender.domain.performance.schedule.entity.PerformanceSchedule;
import back.kalender.domain.performance.schedule.entity.ScheduleStatus;
import back.kalender.domain.performance.schedule.repository.PerformanceScheduleRepository;
import back.kalender.domain.schedule.entity.Schedule;
import back.kalender.domain.schedule.enums.ScheduleCategory;
import back.kalender.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
public class PerformanceBaseInitData implements ApplicationRunner {

    private final ArtistRepository artistRepository;
    private final PerformanceRepository performanceRepository;
    private final PerformanceScheduleRepository performanceScheduleRepository;
    private final ScheduleRepository scheduleRepository;

    private static final Set<String> TARGET = Set.of(
        "BTS", "G-DRAGON", "aespa", "fromis_9", "NCT"
    );

    @Override
    public void run(ApplicationArguments args) {

        if (performanceRepository.count() > 0) return;

        for (Artist artist : artistRepository.findAll()) {

            boolean isTarget =
                    TARGET.contains(artist.getName()) ||
                            artist.getName().equals("NCT WISH");

            if (!isTarget) continue;

            createPerformance(artist);
        }
    }

    private void createPerformance(Artist artist) {

        LocalDate date = LocalDate.of(2025, 12, 15)
            .plusDays((int)(Math.random() * 20));

        Performance performance = performanceRepository.save(
            Performance.builder()
                .performanceHallId(1L)
                .artistId(artist.getId())
                .title(artist.getName() + " 단독 콘서트")
                .posterImageUrl(artist.getImageUrl())
                .startDate(date)
                .endDate(date)
                .runningTime(120)
                .bookingNotice("모바일 티켓 입장")
                .salesStartTime(date.minusDays(14).atTime(10, 0))
                .salesEndTime(date.minusDays(1).atTime(23, 59))
                .build()
        );

        //  Schedule(CONCERT) 생성 (여기서만!)
        scheduleRepository.save(
            Schedule.builder()
                .artistId(artist.getId())
                .performanceId(performance.getId())
                .scheduleCategory(ScheduleCategory.CONCERT)
                .title(performance.getTitle())
                .scheduleTime(date.atTime(18, 0))
                .location("잠실실내체육관")
                .build()
        );

        // 회차 2개
        createSchedule(performance, date, 1, LocalTime.of(18, 0));
        createSchedule(performance, date, 2, LocalTime.of(20, 30));
    }

    private void createSchedule(
            Performance performance,
            LocalDate date,
            int no,
            LocalTime time
    ) {
        performanceScheduleRepository.save(
            PerformanceSchedule.builder()
                .performanceId(performance.getId())
                .performanceDate(date)
                .startTime(time)
                .performanceNo(no)
                .status(ScheduleStatus.AVAILABLE)
                .build()
        );
    }
}