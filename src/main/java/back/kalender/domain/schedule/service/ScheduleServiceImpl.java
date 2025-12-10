package back.kalender.domain.schedule.service;

import back.kalender.domain.artist.entity.ArtistFollowTmp;
import back.kalender.domain.artist.repository.ArtistFollowRepositoryTmp;
import back.kalender.domain.artist.repository.ArtistRepositoryTmp;
import back.kalender.domain.schedule.dto.response.DailySchedulesResponse;
import back.kalender.domain.schedule.dto.response.MonthlyScheduleItem;
import back.kalender.domain.schedule.dto.response.MonthlySchedulesResponse;
import back.kalender.domain.schedule.dto.response.UpcomingEventsResponse;
import back.kalender.domain.schedule.repository.ScheduleRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ArtistFollowRepositoryTmp artistFollowRepository;

    @Override
    public MonthlySchedulesResponse getFollowingSchedules(Long userId, int year, int month) {
        log.info("[Schedule] [GetFollowing] 팔로우 전체 일정 조회 시작 - userId={}, year={}, month={}", userId, year, month);

        List<ArtistFollowTmp> follows = artistFollowRepository.findAllByUserId(userId);
        log.debug("[Schedule] [GetFollowing] 팔로우 아티스트 조회 완료 - count={}", follows.size());

        if (follows.isEmpty()) {
            log.info("[Schedule] [GetFollowing] 팔로우한 아티스트 없음, 빈 리스트 반환 - userId={}", userId);
            return new MonthlySchedulesResponse(Collections.emptyList());
        }

        List<Long> artistIds = follows.stream()
                .map(follow -> follow.getArtist().getId())
                .toList();
        log.debug("[Schedule] [GetFollowing] 대상 아티스트 ID 추출 - artistIds={}", artistIds);

        try {
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDateTime startDateTime = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime endDateTime = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

            log.debug("[Schedule] [GetFollowing] 조회 날짜 범위 계산 - start={}, end={}", startDateTime, endDateTime);

            List<MonthlyScheduleItem> items = scheduleRepository.findMonthlySchedules(
                    artistIds,
                    startDateTime,
                    endDateTime
            );

            log.info(
                    "[Schedule] [GetFollowing] 팔로우 전체 일정 조회 완료 - userId={}, scheduleCount={}",
                    userId, items.size()
            );

            return new MonthlySchedulesResponse(items);

        } catch (DateTimeParseException e) {
            log.error("[Schedule] [GetFollowing] 날짜 파싱 오류 발생 - year={}, month={}", year, month, e);
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public MonthlySchedulesResponse getArtistSchedules(Long userId, Long artistId, int year, int month) {
        return null;
    }

    public DailySchedulesResponse getDailySchedules(Long userId, String date, Optional<Long> artistId) {
        return null;
    }

    public UpcomingEventsResponse getUpcomingEvents(Long userId, Optional<Long> artistId, int limit) {
        return null;
    }
}
