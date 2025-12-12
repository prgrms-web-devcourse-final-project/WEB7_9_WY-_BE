package back.kalender.domain.schedule.service;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.artist.entity.ArtistFollow;
import back.kalender.domain.artist.repository.ArtistFollowRepository;
import back.kalender.domain.schedule.dto.response.*;
import back.kalender.domain.schedule.entity.ScheduleCategory;
import back.kalender.domain.schedule.repository.ScheduleRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService 통합 로직 테스트")
class ScheduleServiceImplTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ArtistFollowRepository artistFollowRepository;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    @Test
    @DisplayName("통합 조회: 전체 아티스트 조회 성공 (D-Day 계산 및 상세 정보 포함 확인)")
    void getIntegratedSchedules_AllArtists_Success() {
        Long userId = 1L;
        int year = 2025;
        int month = 12;

        Artist bts = createArtist(1L, "BTS");
        Artist nj = createArtist(2L, "NewJeans");
        given(artistFollowRepository.findAllByUserId(userId))
                .willReturn(List.of(new ArtistFollow(userId, bts), new ArtistFollow(userId, nj)));

        List<ScheduleResponse> monthlyData = List.of(
                createScheduleDto(100L, 1L, "BTS", "콘서트", "2025-12-05T19:00:00"),
                createScheduleDto(101L, 2L, "NewJeans", "뮤직뱅크", "2025-12-10T17:00:00")
        );
        given(scheduleRepository.findMonthlySchedules(anyList(), any(), any()))
                .willReturn(monthlyData);

        LocalDateTime futureEventTime = LocalDateTime.now().plusDays(3);
        List<UpcomingEventResponse> upcomingRaw = List.of(
                createUpcomingDtoRaw(200L, "BTS", "연말 무대", futureEventTime)
        );
        given(scheduleRepository.findUpcomingEvents(anyList(), any(), any()))
                .willReturn(upcomingRaw);

        FollowingSchedulesListResponse response = scheduleService.getFollowingSchedules(userId, year, month, Optional.empty());

        assertThat(response.monthlySchedules()).hasSize(2);
        assertThat(response.monthlySchedules().get(0).artistName()).isEqualTo("BTS");
        assertThat(response.monthlySchedules().get(0).link()).isPresent();

        assertThat(response.upcomingEvents()).hasSize(1);
        assertThat(response.upcomingEvents().get(0).title()).isEqualTo("연말 무대");

        assertThat(response.upcomingEvents().get(0).daysUntilEvent()).isEqualTo(3L);

        verify(scheduleRepository).findMonthlySchedules(eq(List.of(1L, 2L)), any(), any());
        verify(scheduleRepository).findUpcomingEvents(eq(List.of(1L, 2L)), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("통합 조회: 특정 아티스트 필터링 성공")
    void getIntegratedSchedules_SpecificArtist_Success() {
        Long userId = 1L;
        Long targetArtistId = 1L;

        given(artistFollowRepository.existsByUserIdAndArtistId(userId, targetArtistId))
                .willReturn(true);

        given(scheduleRepository.findMonthlySchedules(anyList(), any(), any()))
                .willReturn(Collections.emptyList());
        given(scheduleRepository.findUpcomingEvents(anyList(), any(), any()))
                .willReturn(Collections.emptyList());

        scheduleService.getFollowingSchedules(userId, 2025, 12, Optional.of(targetArtistId));

        verify(artistFollowRepository, times(0)).findAllByUserId(any());

        verify(scheduleRepository).findMonthlySchedules(eq(List.of(targetArtistId)), any(), any());
        verify(scheduleRepository).findUpcomingEvents(eq(List.of(targetArtistId)), any(), any());
    }

    @Test
    @DisplayName("통합 조회 실패: 팔로우하지 않은 아티스트 요청 (보안/권한 검증)")
    void getIntegratedSchedules_Fail_NotFollowed() {
        Long userId = 1L;
        Long notFollowedId = 99L;

        given(artistFollowRepository.existsByUserIdAndArtistId(userId, notFollowedId))
                .willReturn(false);

        assertThatThrownBy(() ->
                scheduleService.getFollowingSchedules(userId, 2025, 12, Optional.of(notFollowedId))
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.ARTIST_NOT_FOLLOWED.getMessage());

        verify(scheduleRepository, times(0)).findMonthlySchedules(any(), any(), any());
    }

    @Test
    @DisplayName("통합 조회 실패: 유효하지 않은 월 입력")
    void getIntegratedSchedules_Fail_InvalidMonth() {
        Long userId = 1L;
        int invalidMonth = 13;

        assertThatThrownBy(() ->
                scheduleService.getFollowingSchedules(userId, 2025, invalidMonth, Optional.empty())
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }

    @Test
    @DisplayName("통합 조회: 팔로우한 아티스트가 없는 경우 (빈 리스트 반환, DB 조회 X)")
    void getIntegratedSchedules_Success_NoFollows() {
        Long userId = 1L;
        given(artistFollowRepository.findAllByUserId(userId))
                .willReturn(Collections.emptyList());

        FollowingSchedulesListResponse response = scheduleService.getFollowingSchedules(userId, 2025, 12, Optional.empty());

        assertThat(response.monthlySchedules()).isEmpty();
        assertThat(response.upcomingEvents()).isEmpty();

        verify(scheduleRepository, times(0)).findMonthlySchedules(any(), any(), any());
        verify(scheduleRepository, times(0)).findUpcomingEvents(any(), any(), any());
    }

    @Test
    @DisplayName("파티 이벤트 리스트 조회: 성공")
    void getEventLists_Success() {
        Long userId = 1L;
        Artist bts = createArtist(10L, "BTS");
        given(artistFollowRepository.findAllByUserId(userId))
                .willReturn(List.of(new ArtistFollow(userId, bts)));

        List<EventResponse> events = List.of(new EventResponse(100L, "[BTS] 콘서트"));
        given(scheduleRepository.findPartyAvailableEvents(anyList(), anyList(), any(), any()))
                .willReturn(events);

        EventsListResponse response = scheduleService.getEventLists(userId);

        assertThat(response.events()).hasSize(1);
        assertThat(response.events().get(0).title()).isEqualTo("[BTS] 콘서트");
    }

    private Artist createArtist(Long id, String name) {
        Artist artist = new Artist(name, "img_url");
        ReflectionTestUtils.setField(artist, "id", id);
        return artist;
    }

    private ScheduleResponse createScheduleDto(Long id, Long artistId, String artistName, String title, String timeStr) {
        return new ScheduleResponse(
                id, artistId, artistName, title,
                ScheduleCategory.CONCERT,
                LocalDateTime.parse(timeStr),
                null, "https://ticket.com", "서울"
        );
    }

    private UpcomingEventResponse createUpcomingDtoRaw(Long id, String artistName, String title, LocalDateTime time) {
        return new UpcomingEventResponse(
                id, artistName, title,
                ScheduleCategory.CONCERT,
                time,
                null,
                "https://ticket.com",
                null,
                "서울"
        );
    }
}