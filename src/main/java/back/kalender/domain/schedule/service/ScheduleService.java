package back.kalender.domain.schedule.service;

import back.kalender.domain.schedule.dto.response.DailySchedulesResponse;
import back.kalender.domain.schedule.dto.response.MonthlySchedulesResponse;
import back.kalender.domain.schedule.dto.response.UpcomingEventsResponse;

import java.util.Optional;

public interface ScheduleService {
    MonthlySchedulesResponse getFollowingSchedules(Long userId, int year, int month);

    MonthlySchedulesResponse getArtistSchedules(Long userId, Long artistId, int year, int month);

    DailySchedulesResponse getDailySchedules(Long userId, String date, Optional<Long> artistId);

    UpcomingEventsResponse getUpcomingEvents(Long userId, Optional<Long> artistId, int limit);
}
