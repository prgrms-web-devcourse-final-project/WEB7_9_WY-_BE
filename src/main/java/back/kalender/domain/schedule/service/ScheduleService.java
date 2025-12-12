package back.kalender.domain.schedule.service;

import back.kalender.domain.schedule.dto.response.*;

import java.util.Optional;

public interface ScheduleService {

    IntegratedSchedulesListResponse getIntegratedSchedules(Long userId, int year, int month, Optional<Long> artistId);

    EventsListResponse getEventLists(Long userId);
}
