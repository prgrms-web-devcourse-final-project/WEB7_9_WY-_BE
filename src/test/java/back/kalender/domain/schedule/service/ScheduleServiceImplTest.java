package back.kalender.domain.schedule.service;

import back.kalender.domain.artist.entity.ArtistFollowTmp;
import back.kalender.domain.artist.entity.ArtistTmp;
import back.kalender.domain.artist.repository.ArtistFollowRepositoryTmp;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService 테스트")
public class ScheduleServiceImplTest {
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private ArtistFollowRepositoryTmp artistFollowRepository;
    @InjectMocks
    private ScheduleServiceImpl scheduleServiceImpl;

    @Test
    @DisplayName("getFollowingSchedules(팔로우한 전체 아티스트 일정 조회) 테스트")
    void getFollowingSchedules_Success() {
        Long userId = 1L;

        ArtistTmp bts = new ArtistTmp("BTS", "image_url");
        ArtistTmp bp = new ArtistTmp("Black Pink", "image_url");
        ArtistTmp njz = new ArtistTmp("New Jeans", "image_url");
        ArtistTmp nct = new ArtistTmp("NCT WISH", "image_url");

        ReflectionTestUtils.setField(bts, "id", 1L);
        ReflectionTestUtils.setField(bp, "id", 2L);
        ReflectionTestUtils.setField(njz, "id", 3L);
        ReflectionTestUtils.setField(nct, "id", 4L);

        ArtistFollowTmp f1 = new ArtistFollowTmp(userId, bts);
        ArtistFollowTmp f2 = new ArtistFollowTmp(userId, bp);
        ArtistFollowTmp f3 = new ArtistFollowTmp(userId, njz);

        given(artistFollowRepository.findAllByUserId(userId))
                .willReturn(List.of(f1, f2, f3));

        List<MonthlyScheduleItem> dbResult = List.of(
                createDto(100L, 1L, "BTS", "콘서트"),       // BTS 일정
                createDto(101L, 2L, "Black Pink", "컴백"), // BP 일정
                createDto(102L, 3L, "New Jeans", "팬미팅") // NJZ 일정
        );

        given(scheduleRepository.findMonthlySchedules(any(), any(), any()))
                .willReturn(dbResult);

        MonthlySchedulesResponse response = scheduleServiceImpl.getFollowingSchedules(userId, 2025, 12);

        assertThat(response.schedules()).hasSize(3);

        MonthlyScheduleItem item1 = response.schedules().get(0);
        assertThat(item1.artistName()).isEqualTo("BTS");
        assertThat(item1.title()).isEqualTo("콘서트");

        MonthlyScheduleItem item2 = response.schedules().get(1);
        assertThat(item2.artistName()).isEqualTo("Black Pink");
        assertThat(item2.title()).isEqualTo("컴백");

        MonthlyScheduleItem item3 = response.schedules().get(2);
        assertThat(item3.artistName()).isEqualTo("New Jeans");
        assertThat(item3.title()).isEqualTo("팬미팅");
    }

    private MonthlyScheduleItem createDto(Long id, Long artistId, String artistName, String title) {
        return new MonthlyScheduleItem(
                id, artistId, artistName, title,
                ScheduleCategory.CONCERT,
                LocalDateTime.now(),
                Optional.empty(),
                Optional.empty()
        );
    }

    @Test
    @DisplayName("월별 전체 조회 - 실패 (유효하지 않은 월 입력)")
    void getFollowingSchedules_Fail_InvalidMonth() {
        Long userId = 1L;
        int year = 2025;
        int invalidMonth = 13;

        assertThatThrownBy(() ->
                scheduleServiceImpl.getFollowingSchedules(userId, year, invalidMonth)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }

    @Test
    @DisplayName("여러 아티스트 중 특정 아티스트(BTS)만 조회 시, 정확히 해당 ID로만 필터링하여 요청한다.")
    void getArtistSchedules_FilterVerification() {
        Long userId = 1L;
        Long btsId = 1L;
        int year = 2025;
        int month = 11;

        given(artistFollowRepository.existsByUserIdAndArtistId(userId, btsId))
                .willReturn(true);

        List<MonthlyScheduleItem> btsData = List.of(
                createDto(100L, btsId, "BTS", "콘서트"),
                createDto(101L, btsId, "BTS", "팬미팅")
        );

        given(scheduleRepository.findMonthlySchedules(any(), any(), any()))
                .willReturn(btsData);

        MonthlySchedulesResponse response = scheduleServiceImpl.getSchedulesPerArtist(userId, btsId, year, month);

        assertThat(response.schedules()).hasSize(2);
        assertThat(response.schedules().get(0).artistName()).isEqualTo("BTS");

        verify(scheduleRepository).findMonthlySchedules(
                eq(List.of(btsId)),
                any(),
                any()
        );

        verify(artistFollowRepository).existsByUserIdAndArtistId(userId, btsId);
    }

    @Test
    @DisplayName("월별 개별 조회 - 실패 (유효하지 않은 월 입력)")
    void getArtistSchedules_Fail_InvalidMonth() {
        Long userId = 1L;
        Long artistId = 1L;
        int year = 2025;
        int invalidMonth = 0; // 1~12 범위를 벗어남

        assertThatThrownBy(() ->
                scheduleServiceImpl.getSchedulesPerArtist(userId, artistId, year, invalidMonth)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }

    @Test
    @DisplayName("월별 개별 조회 - 실패 (팔로우하지 않은 아티스트 요청)")
    void getArtistSchedules_Fail_NotFollowed() {
        Long userId = 1L;
        Long notFollowedId = 99L;
        int year = 2025;
        int month = 11;

        given(artistFollowRepository.existsByUserIdAndArtistId(userId, notFollowedId))
                .willReturn(false);

        assertThatThrownBy(() ->
                scheduleServiceImpl.getSchedulesPerArtist(userId, notFollowedId, year, month)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.ARTIST_NOT_FOLLOWED.getMessage());

        verify(scheduleRepository, times(0)).findMonthlySchedules(any(), any(), any());
    }
    @Test
    @DisplayName("하루 상세 조회 - 성공 (전체 아티스트)")
    void getDailySchedules_Success_AllArtists() {
        Long userId = 1L;
        String dateStr = "2025-12-15";

        ArtistTmp bts = new ArtistTmp("BTS", "img");
        ReflectionTestUtils.setField(bts, "id", 1L);
        ArtistFollowTmp f1 = new ArtistFollowTmp(userId, bts);

        ArtistTmp bp = new ArtistTmp("BP", "img");
        ReflectionTestUtils.setField(bp, "id", 2L);
        ArtistFollowTmp f2 = new ArtistFollowTmp(userId, bp);

        given(artistFollowRepository.findAllByUserId(userId))
                .willReturn(List.of(f1, f2));

        List<DailyScheduleItem> dbResult = List.of(
                createDailyDto(100L, "BTS", "콘서트"),
                createDailyDto(101L, "BP", "방송")
        );

        given(scheduleRepository.findDailySchedules(any(), any(), any()))
                .willReturn(dbResult);

        DailySchedulesResponse response = scheduleServiceImpl.getDailySchedules(userId, dateStr, Optional.empty());

        assertThat(response.dailySchedules()).hasSize(2);

        verify(scheduleRepository).findDailySchedules(
                eq(List.of(1L, 2L)),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("하루 상세 조회 - 성공 (특정 아티스트 필터링)")
    void getDailySchedules_Success_SpecificArtist() {
        Long userId = 1L;
        Long targetId = 1L; // BTS
        String dateStr = "2025-12-15";

        given(artistFollowRepository.existsByUserIdAndArtistId(userId, targetId))
                .willReturn(true);

        List<DailyScheduleItem> dbResult = List.of(
                createDailyDto(100L, "BTS", "콘서트")
        );

        given(scheduleRepository.findDailySchedules(any(), any(), any()))
                .willReturn(dbResult);

        DailySchedulesResponse response = scheduleServiceImpl.getDailySchedules(userId, dateStr, Optional.of(targetId));

        assertThat(response.dailySchedules()).hasSize(1);
        assertThat(response.dailySchedules().get(0).artistName()).isEqualTo("BTS");

        verify(scheduleRepository).findDailySchedules(
                eq(List.of(targetId)),
                any(),
                any()
        );
        verify(artistFollowRepository, times(0)).findAllByUserId(any());
    }

    @Test
    @DisplayName("하루 상세 조회 - 실패 (날짜 포맷 오류)")
    void getDailySchedules_Fail_InvalidDate() {
        Long userId = 1L;
        String invalidDate = "2025/12/15";

        assertThatThrownBy(() ->
                scheduleServiceImpl.getDailySchedules(userId, invalidDate, Optional.empty())
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }

    @Test
    @DisplayName("하루 상세 조회 - 실패 (팔로우하지 않은 아티스트 요청)")
    void getDailySchedules_Fail_NotFollowed() {
        Long userId = 1L;
        Long notFollowedId = 99L;
        String dateStr = "2025-12-15";

        given(artistFollowRepository.existsByUserIdAndArtistId(userId, notFollowedId))
                .willReturn(false);

        assertThatThrownBy(() ->
                scheduleServiceImpl.getDailySchedules(userId, dateStr, Optional.of(notFollowedId))
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.ARTIST_NOT_FOLLOWED.getMessage());
    }

    private DailyScheduleItem createDailyDto(Long id, String artistName, String title) {
        return new DailyScheduleItem(
                id, artistName, title,
                ScheduleCategory.CONCERT,
                LocalDateTime.now(),
                null, null, "장소"
        );
    }

    @Test
    @DisplayName("다가오는 일정 조회 - 성공 (전체 아티스트 & D-Day 계산 검증)")
    void getUpcomingEvents_Success_AllArtists() {
        Long userId = 1L;
        int limit = 5;

        ArtistTmp bts = new ArtistTmp("BTS", "img"); ReflectionTestUtils.setField(bts, "id", 1L);
        ArtistFollowTmp f1 = new ArtistFollowTmp(userId, bts);

        given(artistFollowRepository.findAllByUserId(userId)).willReturn(List.of(f1));

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);

        List<UpcomingEventItem> dbResult = List.of(
                new UpcomingEventItem(
                        100L,
                        "BTS",
                        "콘서트",
                        ScheduleCategory.CONCERT,
                        tomorrow,
                        Optional.empty(),
                        Optional.empty(),
                        0L,
                        Optional.empty()
                )
        );

        given(scheduleRepository.findUpcomingEvents(any(), any(), any()))
                .willReturn(dbResult);

        UpcomingEventsResponse response = scheduleServiceImpl.getUpcomingEvents(userId, Optional.empty(), limit);

        assertThat(response.upcomingEvents()).hasSize(1);

        assertThat(response.upcomingEvents().get(0).daysUntilEvent()).isEqualTo(1L);

        verify(scheduleRepository).findUpcomingEvents(
                eq(List.of(1L)),
                any(LocalDateTime.class),
                refEq(org.springframework.data.domain.PageRequest.of(0, limit))
        );
    }

    @Test
    @DisplayName("다가오는 일정 조회 - 성공 (특정 아티스트 필터링)")
    void getUpcomingEvents_Success_SpecificArtist() {
        Long userId = 1L;
        Long targetId = 1L;
        int limit = 10;

        given(artistFollowRepository.existsByUserIdAndArtistId(userId, targetId))
                .willReturn(true);

        given(scheduleRepository.findUpcomingEvents(any(), any(), any()))
                .willReturn(List.of());

        scheduleServiceImpl.getUpcomingEvents(userId, Optional.of(targetId), limit);

        verify(scheduleRepository).findUpcomingEvents(
                eq(List.of(targetId)),
                any(),
                any()
        );
        verify(artistFollowRepository, times(0)).findAllByUserId(any());
    }

    @Test
    @DisplayName("다가오는 일정 조회 - 실패 (Limit 값 오류)")
    void getUpcomingEvents_Fail_InvalidLimit() {
        Long userId = 1L;
        int invalidLimit = -1;

        assertThatThrownBy(() ->
                scheduleServiceImpl.getUpcomingEvents(userId, Optional.empty(), invalidLimit)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }

    @Test
    @DisplayName("다가오는 일정 조회 - 실패 (팔로우하지 않은 아티스트)")
    void getUpcomingEvents_Fail_NotFollowed() {
        Long userId = 1L;
        Long notFollowedId = 99L;

        given(artistFollowRepository.existsByUserIdAndArtistId(userId, notFollowedId))
                .willReturn(false);

        assertThatThrownBy(() ->
                scheduleServiceImpl.getUpcomingEvents(userId, Optional.of(notFollowedId), 10)
        )
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining(ErrorCode.ARTIST_NOT_FOLLOWED.getMessage());
    }
}
