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
@Profile({"prod", "dev"})
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

    // 예매 시작 기준일
    private static final LocalDate BOOKING_BASE_DATE = LocalDate.of(2026, 1, 7);

    //  공연 날짜 범위
    private static final LocalDate PERFORMANCE_START_DATE = LocalDate.of(2026, 1, 15);
    private static final LocalDate PERFORMANCE_END_DATE   = LocalDate.of(2026, 1, 31);

    @Override
    public void run(ApplicationArguments args) {

        if (performanceRepository.count() > 0) return;

        int index = 0;

        for (Artist artist : artistRepository.findAll()) {

            boolean isTarget =
                    TARGET.contains(artist.getName()) ||
                            artist.getName().equals("NCT WISH");

            if (!isTarget) continue;

            createPerformance(artist, index);
            index++;
        }
    }

    private void createPerformance(Artist artist, int index) {

        //  예매 시작일: 1/7, 1/9, 1/11, 1/13, 1/15
        LocalDate bookingStartDate =
                BOOKING_BASE_DATE.plusDays(index * 2);

        //  공연일: 1/15 ~ 1/31 랜덤
        int range = (int)(
                PERFORMANCE_END_DATE.toEpochDay()
                        - PERFORMANCE_START_DATE.toEpochDay()
                        + 1
        );

        LocalDate performanceDate =
                PERFORMANCE_START_DATE.plusDays((int)(Math.random() * range));

        Performance performance = performanceRepository.save(
                Performance.builder()
                        .performanceHallId(1L)
                        .artistId(artist.getId())
                        .title(artist.getName() + " 단독 콘서트")
                        .posterImageUrl(artist.getImageUrl())
                        .startDate(performanceDate)
                        .endDate(performanceDate)
                        .runningTime(120)
                        .bookingNotice("모바일 티켓 입장")

                        //  공연별로 다른 예매 시작일
                        .salesStartTime(bookingStartDate.atTime(10, 0))
                        .salesEndTime(performanceDate.minusDays(1).atTime(23, 59))

                        .build()
        );

        //  캘린더 Schedule (CONCERT)
        scheduleRepository.save(
                Schedule.builder()
                        .artistId(artist.getId())
                        .performanceId(performance.getId())
                        .scheduleCategory(ScheduleCategory.CONCERT)
                        .title(performance.getTitle())
                        .scheduleTime(performanceDate.atTime(18, 0))
                        .location("잠실실내체육관")
                        .build()
        );

        // 🎶 회차 2개
        createSchedule(performance, performanceDate, 1, LocalTime.of(18, 0));
        createSchedule(performance, performanceDate, 2, LocalTime.of(20, 30));
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