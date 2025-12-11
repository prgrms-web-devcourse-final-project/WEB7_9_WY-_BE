package back.kalender.domain.schedule.service;

import back.kalender.domain.artist.entity.ArtistFollow;
import back.kalender.domain.artist.repository.ArtistFollowRepository;
import back.kalender.domain.schedule.dto.response.*;
import back.kalender.domain.schedule.entity.ScheduleCategory;
import back.kalender.domain.schedule.repository.ScheduleRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final ArtistFollowRepository artistFollowRepository;

    @Override
    public MonthlySchedulesListResponse getFollowingSchedules(Long userId, int year, int month) {
        log.info("[Schedule] [GetFollowing] 팔로우 전체 일정 조회 시작 - userId={}, year={}, month={}",
                userId, year, month);

        if (month < 1 || month > 12) {
            log.error("[Schedule] [GetFollowing] 유효하지 않은 월 입력 - month={}", month);
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Long> artistIds = getFollowedArtistIds(userId);

        if (artistIds.isEmpty()) {
            log.info("[Schedule] [GetFollowing] 팔로우한 아티스트 없음, 빈 리스트 반환 - userId={}", userId);
            return new MonthlySchedulesListResponse(Collections.emptyList());
        }

        log.debug("[Schedule] [GetFollowing] 대상 아티스트 ID 추출 - count={}, ids={}", artistIds.size(), artistIds);

        try {
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDateTime startDateTime = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime endDateTime = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

            log.debug("[Schedule] [GetFollowing] 조회 날짜 범위 계산 - start={}, end={}", startDateTime, endDateTime);

            List<MonthlyScheduleResponse> items = scheduleRepository.findMonthlySchedules(
                    artistIds,
                    startDateTime,
                    endDateTime
            );

            log.info(
                    "[Schedule] [GetFollowing] 팔로우 전체 일정 조회 완료 - userId={}, scheduleCount={}",
                    userId, items.size()
            );

            return new MonthlySchedulesListResponse(items);

        } catch (DateTimeParseException e) {
            log.error("[Schedule] [GetFollowing] 날짜 파싱 오류 발생 - year={}, month={}", year, month, e);
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Override
    public MonthlySchedulesListResponse getSchedulesPerArtist(Long userId, Long artistId, int year, int month) {
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

            List<MonthlyScheduleResponse> items = scheduleRepository.findMonthlySchedules(
                    List.of(artistId),
                    startDateTime,
                    endDateTime
            );

            log.info(
                    "[Schedule] [GetPerArtist] 아티스트별 월별 일정 조회 완료 - userId={}, artistId={}, scheduleCount={}",
                    userId, artistId, items.size()
            );

            return new MonthlySchedulesListResponse(items);

        } catch (DateTimeParseException e) {
            log.error("[Schedule] [GetPerArtist] 날짜 파싱 오류 발생 - year={}, month={}", year, month, e);
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Override
    public DailySchedulesListResponse getDailySchedules(Long userId, String date, Optional<Long> artistId) {
        log.info("[Schedule] [GetDaily] 하루 상세 일정 조회 시작 - userId={}, date={}, specificArtist={}",
                userId, date, artistId.orElse(null));

        LocalDateTime startOfDay;
        LocalDateTime endOfDay;

        try {
            LocalDate targetDate = LocalDate.parse(date);
            startOfDay = targetDate.atStartOfDay();
            endOfDay = targetDate.atTime(LocalTime.MAX);

            log.debug("[Schedule] [GetDaily] 조회 시간 범위 계산 - start={}, end={}", startOfDay, endOfDay);

        } catch (DateTimeParseException e) {
            log.error("[Schedule] [GetDaily] 날짜 파싱 오류 - date={}", date, e);
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Long> targetArtistIds;

        if (artistId.isPresent()) {
            Long id = artistId.get();

            boolean isFollowing = artistFollowRepository.existsByUserIdAndArtistId(userId, id);
            if (!isFollowing) {
                log.warn("[Schedule] [GetDaily] 팔로우 관계 없음 - userId={}, artistId={}", userId, id);
                throw new ServiceException(ErrorCode.ARTIST_NOT_FOLLOWED);
            }

            targetArtistIds = List.of(id);
            log.debug("[Schedule] [GetDaily] 단일 아티스트 필터링 적용 - artistId={}", id);

        } else {
            targetArtistIds = getFollowedArtistIds(userId);

            if (targetArtistIds.isEmpty()) {
                log.info("[Schedule] [GetDaily] 팔로우한 아티스트 없음 - 빈 리스트 반환");
                return new DailySchedulesListResponse(Collections.emptyList());
            }

            log.debug("[Schedule] [GetDaily] 전체 팔로우 아티스트 적용 - count={}", targetArtistIds.size());
        }

        List<DailyScheduleResponse> items = scheduleRepository.findDailySchedules(
                targetArtistIds,
                startOfDay,
                endOfDay
        );

        log.info("[Schedule] [GetDaily] 하루 상세 일정 조회 완료 - count={}", items.size());

        return new DailySchedulesListResponse(items);
    }

    public UpcomingEventsListResponse getUpcomingEvents(Long userId, Optional<Long> artistId, int limit) {
        log.info("[Schedule] [GetUpcoming] 다가오는 일정 조회 시작 - userId={}, specificArtist={}, limit={}",
                userId, artistId.orElse(null), limit);

        if (limit <= 0) {
            log.error("[Schedule] [GetUpcoming] 유효하지 않은 limit 값 - limit={}", limit);
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Long> targetArtistIds;

        if (artistId.isPresent()) {
            Long id = artistId.get();

            boolean isFollowing = artistFollowRepository.existsByUserIdAndArtistId(userId, id);
            if (!isFollowing) {
                log.warn("[Schedule] [GetUpcoming] 팔로우 관계 없음 - userId={}, artistId={}", userId, id);
                throw new ServiceException(ErrorCode.ARTIST_NOT_FOLLOWED);
            }
            targetArtistIds = List.of(id);
            log.debug("[Schedule] [GetUpcoming] 단일 아티스트 필터링 적용 - artistId={}", id);

        } else {
            targetArtistIds = getFollowedArtistIds(userId);

            if (targetArtistIds.isEmpty()) {
                log.info("[Schedule] [GetUpcoming] 팔로우한 아티스트 없음 - 빈 리스트 반환");
                return new UpcomingEventsListResponse(Collections.emptyList());
            }

            log.debug("[Schedule] [GetUpcoming] 전체 팔로우 아티스트 적용 - count={}", targetArtistIds.size());
        }

        List<UpcomingEventResponse> rawItems = scheduleRepository.findUpcomingEvents(
                targetArtistIds,
                LocalDateTime.now(),
                PageRequest.of(0, limit)
        );

        LocalDate today = LocalDate.now();

        List<UpcomingEventResponse> processedItems = rawItems.stream()
                .map(item -> {
                    long dDay = java.time.temporal.ChronoUnit.DAYS.between(today, item.scheduleTime().toLocalDate());

                    return new UpcomingEventResponse(
                            item.scheduleId(),
                            item.artistName(),
                            item.title(),
                            item.scheduleCategory(),
                            item.scheduleTime(),
                            item.performanceId().orElse(null),
                            item.link().orElse(null),
                            dDay,
                            item.location().orElse(null)
                    );
                })
                .toList();

        log.info("[Schedule] [GetUpcoming] 다가오는 일정 조회 완료 - count={}", processedItems.size());

        return new UpcomingEventsListResponse(processedItems);
    }

    private List<Long> getFollowedArtistIds(Long userId) {
        List<ArtistFollow> follows = artistFollowRepository.findAllByUserId(userId);

        if (follows.isEmpty()) {
            return Collections.emptyList();
        }

        return follows.stream()
                .map(follow -> follow.getArtist().getId())
                .toList();
    }

    @Override
    public EventsListResponse getEventLists(Long userId) {
        log.info("[Schedule] [GetEventLists] 이벤트 리스트 조회 시작 - userId={}", userId);

        List<Long> targetArtistIds = getFollowedArtistIds(userId);

        if (targetArtistIds.isEmpty()) {
            log.info("[Schedule] [GetEventLists] 팔로우한 아티스트 없음 - 빈 리스트 반환");
            return new EventsListResponse(Collections.emptyList());
        }

        List<ScheduleCategory> partyCategories = List.of(
                ScheduleCategory.CONCERT,
                ScheduleCategory.FAN_MEETING,
                ScheduleCategory.FESTIVAL,
                ScheduleCategory.AWARD_SHOW,
                ScheduleCategory.FAN_SIGN,
                ScheduleCategory.BROADCAST
        );

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(31);

        List<EventResponse> items = scheduleRepository.findPartyAvailableEvents(
                targetArtistIds,
                partyCategories,
                now,
                endDate
        );

        log.info("[Schedule] [GetEventLists] 파티 생성 기능 일정 목록 조회 완료 - count={}", items.size());

        return new EventsListResponse(items);
    }
}
