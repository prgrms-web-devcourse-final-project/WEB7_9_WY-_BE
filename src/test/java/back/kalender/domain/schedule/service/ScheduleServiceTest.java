package back.kalender.domain.schedule.service;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.artist.entity.ArtistFollow;
import back.kalender.domain.artist.repository.ArtistFollowRepository;
import back.kalender.domain.artist.repository.ArtistRepository;
import back.kalender.domain.schedule.dto.response.*;
import back.kalender.domain.schedule.entity.Schedule;
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

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService 로직 단위 테스트")
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ArtistFollowRepository artistFollowRepository;

    @Mock
    private ArtistRepository artistRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    @DisplayName("통합 조회: 전체 아티스트 조회 성공")
    void getFollowingSchedules_AllArtists_Success() {
        Long userId = 1L;
        int year = 2025;
        int month = 12;

        given(artistFollowRepository.findAllByUserId(userId))
                .willReturn(List.of(
                        new ArtistFollow(userId, 1L),
                        new ArtistFollow(userId, 2L)
                ));

        Schedule schedule1 = createScheduleEntity(100L, 1L, "BTS 콘서트", LocalDateTime.of(2025, 12, 15, 19, 0));
        Schedule schedule2 = createScheduleEntity(101L, 2L, "NewJeans 팬싸", LocalDateTime.of(2025, 12, 20, 14, 0));

        given(scheduleRepository.findMonthlySchedules(anyList(), any(), any()))
                .willReturn(List.of(schedule1, schedule2));

        Schedule upcomingSchedule = createScheduleEntity(200L, 1L, "BTS 연말무대", LocalDateTime.now().plusDays(3));
        given(scheduleRepository.findUpcomingEvents(anyList(), any(), any(Pageable.class)))
                .willReturn(List.of(upcomingSchedule));

        Artist bts = createArtistEntity(1L, "BTS");
        Artist newJeans = createArtistEntity(2L, "NewJeans");

        given(artistRepository.findAllById(anyCollection()))
                .willReturn(List.of(bts, newJeans));

        FollowingSchedulesListResponse response =
                scheduleService.getFollowingSchedules(userId, year, month, null);

        assertThat(response.monthlySchedules()).hasSize(2);
        assertThat(response.monthlySchedules().get(0).artistName()).isEqualTo("BTS");
        assertThat(response.monthlySchedules().get(1).artistName()).isEqualTo("NewJeans");

        assertThat(response.upcomingEvents()).hasSize(1);
        assertThat(response.upcomingEvents().get(0).daysUntilEvent()).isEqualTo(3);

        verify(scheduleRepository).findMonthlySchedules(anyList(), any(), any());
        verify(artistRepository).findAllById(anyCollection());
    }

    @Test
    @DisplayName("통합 조회: 특정 아티스트 필터링 성공")
    void getFollowingSchedules_SpecificArtist_Success() {
        Long userId = 1L;
        Long artistId = 1L;

        given(artistFollowRepository.existsByUserIdAndArtistId(userId, artistId))
                .willReturn(true);

        given(scheduleRepository.findMonthlySchedules(eq(List.of(artistId)), any(), any()))
                .willReturn(Collections.emptyList());
        given(scheduleRepository.findUpcomingEvents(eq(List.of(artistId)), any(), any()))
                .willReturn(Collections.emptyList());

        given(artistRepository.findAllById(anyCollection())).willReturn(Collections.emptyList());

        scheduleService.getFollowingSchedules(userId, 2025, 12, artistId);

        verify(artistFollowRepository, never()).findAllByUserId(any());
        verify(scheduleRepository).findMonthlySchedules(eq(List.of(artistId)), any(), any());
    }

    @Test
    @DisplayName("통합 조회 실패: 팔로우하지 않은 아티스트 필터링 시도")
    void getFollowingSchedules_Fail_NotFollowed() {
        Long userId = 1L;
        Long artistId = 99L;

        given(artistFollowRepository.existsByUserIdAndArtistId(userId, artistId))
                .willReturn(false);

        assertThatThrownBy(() ->
                scheduleService.getFollowingSchedules(userId, 2025, 12, artistId)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.ARTIST_NOT_FOLLOWED.getMessage());

        verify(scheduleRepository, never()).findMonthlySchedules(any(), any(), any());
    }

    @Test
    @DisplayName("통합 조회 실패: 잘못된 월 입력")
    void getFollowingSchedules_Fail_InvalidMonth() {
        assertThatThrownBy(() ->
                scheduleService.getFollowingSchedules(1L, 2025, 13, null)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }

    @Test
    @DisplayName("통합 조회: 팔로우한 아티스트가 없는 경우 (빈 결과 반환)")
    void getFollowingSchedules_NoFollow() {
        Long userId = 1L;
        given(artistFollowRepository.findAllByUserId(userId))
                .willReturn(Collections.emptyList());

        FollowingSchedulesListResponse response =
                scheduleService.getFollowingSchedules(userId, 2025, 12, null);

        assertThat(response.monthlySchedules()).isEmpty();
        assertThat(response.upcomingEvents()).isEmpty();

        verify(scheduleRepository, never()).findMonthlySchedules(any(), any(), any());
    }

    @Test
    @DisplayName("파티 이벤트 리스트 조회 성공: 제목 포맷팅 검증")
    void getEventLists_Success() {
        Long userId = 1L;

        given(artistFollowRepository.findAllByUserId(userId))
                .willReturn(List.of(new ArtistFollow(userId, 1L)));

        Schedule schedule = createScheduleEntity(100L, 1L, "월드 투어", LocalDateTime.now().plusDays(10));
        given(scheduleRepository.findPartyAvailableEvents(anyList(), anyList(), any(), any()))
                .willReturn(List.of(schedule));

        Artist artist = createArtistEntity(1L, "BTS");
        given(artistRepository.findAllById(anyCollection()))
                .willReturn(List.of(artist));

        EventsListResponse response = scheduleService.getEventLists(userId);

        assertThat(response.events()).hasSize(1);
        assertThat(response.events().get(0).title()).isEqualTo("[BTS] 월드 투어");
    }

    private Artist createArtistEntity(Long id, String name) {
        Artist artist = new Artist(name, "imageUrl");
        ReflectionTestUtils.setField(artist, "id", id);
        return artist;
    }

    private Schedule createScheduleEntity(Long id, Long artistId, String title, LocalDateTime time) {
        try {
            Constructor<Schedule> constructor = Schedule.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            Schedule schedule = constructor.newInstance();

            ReflectionTestUtils.setField(schedule, "id", id);
            ReflectionTestUtils.setField(schedule, "artistId", artistId);
            ReflectionTestUtils.setField(schedule, "title", title);
            ReflectionTestUtils.setField(schedule, "scheduleCategory", ScheduleCategory.CONCERT);
            ReflectionTestUtils.setField(schedule, "scheduleTime", time);
            ReflectionTestUtils.setField(schedule, "location", "Seoul");

            return schedule;
        } catch (Exception e) {
            throw new RuntimeException("Schedule 엔티티 생성 실패", e);
        }
    }
}