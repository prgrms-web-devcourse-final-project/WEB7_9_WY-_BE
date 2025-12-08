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

    // 팔로우하는 아티스트들의 월별 일정 가져오기
    public MonthlySchedulesResponse getFollowingSchedules(Long userId, int year, int month) {
        return null;
    }

    // 팔로우하는 아티스트 중 특정 아티스트의 월별 일정 가져오기
    public MonthlySchedulesResponse getArtistSchedules(Long userId, Long artistId, int year, int month) {
        return null;
    }


    public DailySchedulesResponse getDailySchedules(Long userId, String date, Optional<Long> artistId) {
        return null;
    }
}
