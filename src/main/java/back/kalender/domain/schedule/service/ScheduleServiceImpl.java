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
    public FollowingSchedulesListResponse getFollowingSchedules(Long userId, int year, int month, Optional<Long> artistId) {
        log.info("[Schedule] [Integrated] 통합 일정 조회 요청 진입 - userId: {}, year: {}, month: {}, filterArtistId: {}",
                userId, year, month, artistId.orElse(null));
        if (month < 1 || month > 12) {
            log.error("[Schedule] [Validation] 유효하지 않은 월 입력 - month: {}", month);
            throw new ServiceException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Long> targetArtistIds;

        if (artistId.isPresent()) {
            Long id = artistId.get();
            log.info("[Schedule] [FilterArtist] 특정 아티스트 필터링 모드 진입 - artistId: {}", id);

            boolean isFollowing = artistFollowRepository.existsByUserIdAndArtistId(userId, id);

            if (!isFollowing) {
                log.warn("[Schedule] [FilterArtist] 조회 권한 없음: 팔로우하지 않은 아티스트 - userId: {}, artistId: {}", userId, id);
                throw new ServiceException(ErrorCode.ARTIST_NOT_FOLLOWED);
            }
            targetArtistIds = List.of(id);
        } else {
            log.info("[Schedule] [GetFollowing] 전체 팔로우 아티스트 조회 모드 진입");
            targetArtistIds = getFollowedArtistIds(userId);
        }

        if (targetArtistIds.isEmpty()) {
            log.info("[Schedule] [Integrated] 조회 대상 아티스트가 없음 (팔로우 0명) - 빈 결과 반환");
            return new FollowingSchedulesListResponse(Collections.emptyList(), Collections.emptyList());
        }

        log.info("[Schedule] [Integrated] 최종 DB 조회 대상 아티스트 ID 목록 (총 {}명): {}", targetArtistIds.size(), targetArtistIds);

        // 월별 일정 조회
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

        log.info("[Schedule] [Monthly] 월별 일정 DB 조회 요청 - 기간: {} ~ {}", startOfMonth, endOfMonth);

        List<ScheduleResponse> monthlyData = scheduleRepository.findMonthlySchedules(
                targetArtistIds,
                startOfMonth,
                endOfMonth
        );

        log.info("[Schedule] [Monthly] 조회 완료 - 건수: {}", monthlyData.size());

        // 다가오는 일정 조회
        int upcomingLimit = 10;
        log.info("[Schedule] [Upcoming] 다가오는 일정 DB 조회 요청 - Limit: {}", upcomingLimit);

        List<UpcomingEventResponse> upcomingRaw = scheduleRepository.findUpcomingEvents(
                targetArtistIds,
                LocalDateTime.now(),
                PageRequest.of(0, upcomingLimit)
        );

        LocalDate today = LocalDate.now();
        List<UpcomingEventResponse> upcomingData = upcomingRaw.stream()
                .map(item -> new UpcomingEventResponse(
                        item.scheduleId(),
                        item.artistName(),
                        item.title(),
                        item.scheduleCategory(),
                        item.scheduleTime(),
                        item.performanceId(),
                        item.link(),
                        java.time.temporal.ChronoUnit.DAYS.between(today, item.scheduleTime().toLocalDate()),
                        item.location()
                ))
                .toList();

        log.info("[Schedule] [Upcoming] 데이터 가공 완료 (D-Day 계산) - 건수: {}", upcomingData.size());

        log.info("[Schedule] [Integrated] 요청 처리 최종 완료 - Response 반환");

        return new FollowingSchedulesListResponse(monthlyData, upcomingData);
    }

    private List<Long> getFollowedArtistIds(Long userId) {
        List<ArtistFollow> follows = artistFollowRepository.findAllByUserId(userId);

        if (follows.isEmpty()) {
            log.debug("[Schedule] [Helper] userId: {} 의 팔로우 목록이 비어있음", userId);
            return Collections.emptyList();
        }

        List<Long> ids = follows.stream()
                .map(follow -> follow.getArtist().getId())
                .toList();

        log.debug("[Schedule] [Helper] userId: {} 의 팔로우 아티스트 ID 추출 완료 ({}명)", userId, ids.size());
        return ids;
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
