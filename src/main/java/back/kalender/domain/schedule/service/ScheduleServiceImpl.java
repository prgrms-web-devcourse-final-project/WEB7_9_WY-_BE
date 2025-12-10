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
        log.info("[Schedule] [GetFollowing] 팔로우 전체 일정 조회 시작 - userId={}, year={}, month={}",
                userId, year, month);

        if (month < 1 || month > 12) {
            log.error("[Schedule] [GetFollowing] 유효하지 않은 월 입력 - month={}", month);
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }

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

    @Override
    public MonthlySchedulesResponse getSchedulesPerArtist(Long userId, Long artistId, int year, int month) {
        log.info("[Schedule] [GetPerArtist] 아티스트별 월별 일정 조회 시작 - userId={}, artistId={}, year={}, month={}",
                userId, artistId, year, month);

        if (month < 1 || month > 12) {
            log.error("[Schedule] [GetPerArtist] 유효하지 않은 월 입력 - month={}", month);
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }

        boolean isFollowing = artistFollowRepository.existsByUserIdAndArtistId(userId, artistId);

        if (!isFollowing) {
            log.warn("[Schedule] [GetPerArtist] 팔로우 관계 없음 (조회 권한 없음) - userId={}, artistId={}", userId, artistId);
            throw new ServiceException(ErrorCode.ARTIST_NOT_FOLLOWED);
        }

        log.debug("[Schedule] [GetPerArtist] 팔로우 관계 확인 완료 - userId={}, artistId={}", userId, artistId);

        try {
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDateTime startDateTime = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime endDateTime = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

            log.debug("[Schedule] [GetPerArtist] 조회 날짜 범위 계산 - start={}, end={}", startDateTime, endDateTime);

            List<MonthlyScheduleItem> items = scheduleRepository.findMonthlySchedules(
                    List.of(artistId),
                    startDateTime,
                    endDateTime
            );

            log.info(
                    "[Schedule] [GetPerArtist] 아티스트별 월별 일정 조회 완료 - userId={}, artistId={}, scheduleCount={}",
                    userId, artistId, items.size()
            );

            return new MonthlySchedulesResponse(items);

        } catch (DateTimeParseException e) {
            log.error("[Schedule] [GetPerArtist] 날짜 파싱 오류 발생 - year={}, month={}", year, month, e);
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public DailySchedulesResponse getDailySchedules(Long userId, String date, Optional<Long> artistId) {
        return null;
    }

    public UpcomingEventsResponse getUpcomingEvents(Long userId, Optional<Long> artistId, int limit) {
        return null;
    }
}
