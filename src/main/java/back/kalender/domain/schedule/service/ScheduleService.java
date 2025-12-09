package back.kalender.domain.schedule.service;

import back.kalender.domain.schedule.dto.response.DailySchedulesResponse;
import back.kalender.domain.schedule.dto.response.MonthlySchedulesResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ScheduleService {
//    private final ScheduleRepository scheduleRepository;
//    private final ArtistRepositoryTmp artistRepository;

//    public ScheduleService(ScheduleRepository scheduleRepository, ArtistRepositoryTmp artistRepository) {
//        this.scheduleRepository = scheduleRepository;
//        this.artistRepository = artistRepository;
//    }

    public MonthlySchedulesResponse getFollowingSchedules(Long userId, int year, int month) {
        return null;
    }

    public MonthlySchedulesResponse getArtistSchedules(Long userId, Long artistId, int year, int month) {
        return null;
    }


    public DailySchedulesResponse getDailySchedules(Long userId, String date, Optional<Long> artistId) {
        return null;
    }
}
