package back.kalender.domain.schedule.service;

import back.kalender.domain.schedule.dto.response.*;

import java.util.Optional;

public interface ScheduleService {
    MonthlySchedulesListResponse getFollowingSchedules(Long userId, int year, int month);

    MonthlySchedulesListResponse getSchedulesPerArtist(Long userId, Long artistId, int year, int month);

    DailySchedulesListResponse getDailySchedules(Long userId, String date, Optional<Long> artistId);

    UpcomingEventsListResponse getUpcomingEvents(Long userId, Optional<Long> artistId, int limit);

    EventsListResponse getEventLists(Long userId);
}
