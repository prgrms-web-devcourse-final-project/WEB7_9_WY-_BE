//package back.kalender.domain.schedule.service;
//
//import back.kalender.domain.artist.entity.Artist;
//import back.kalender.domain.artist.entity.ArtistFollow;
//import back.kalender.domain.artist.repository.ArtistFollowRepository;
//import back.kalender.domain.artist.repository.ArtistRepository;
//import back.kalender.domain.schedule.dto.response.*;
//import back.kalender.domain.schedule.entity.ScheduleCategory;
//import back.kalender.domain.schedule.repository.ScheduleRepository;
//import back.kalender.domain.user.entity.User;
//import back.kalender.domain.user.repository.UserRepository;
//import back.kalender.global.exception.ErrorCode;
//import back.kalender.global.exception.ServiceException;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Pageable;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("ScheduleService 통합 로직 테스트")
//class ScheduleServiceImplTest {
//
//    @Mock
//    private ScheduleRepository scheduleRepository;
//
//    @Mock
//    private ArtistFollowRepository artistFollowRepository;
//
//    @Mock
//    private ArtistRepository artistRepository;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @InjectMocks
//    private ScheduleServiceImpl scheduleService;
//
//    /* ======================
//       통합 조회 - 전체 아티스트
//       ====================== */
//
//    @Test
//    @DisplayName("통합 조회: 전체 아티스트 조회 성공")
//    void getIntegratedSchedules_AllArtists_Success() {
//        Long userId = 1L;
//        int year = 2025;
//        int month = 12;
//
//        User user = mock(User.class);
//        Artist bts = createArtist(1L, "BTS");
//        Artist nj = createArtist(2L, "NewJeans");
//
//        given(userRepository.getReferenceById(userId)).willReturn(user);
//        given(artistFollowRepository.findAllByUser(user))
//                .willReturn(List.of(
//                        new ArtistFollow(user, bts),
//                        new ArtistFollow(user, nj)
//                ));
//
//        given(scheduleRepository.findMonthlySchedules(anyList(), any(), any()))
//                .willReturn(List.of(
//                        createScheduleDto(100L, 1L, "BTS"),
//                        createScheduleDto(101L, 2L, "NewJeans")
//                ));
//
//        LocalDateTime future = LocalDateTime.now().plusDays(3);
//        given(scheduleRepository.findUpcomingEvents(anyList(), any(), any()))
//                .willReturn(List.of(
//                        createUpcomingDtoRaw(200L, "BTS", future)
//                ));
//
//        FollowingSchedulesListResponse response =
//                scheduleService.getFollowingSchedules(userId, year, month, Optional.empty());
//
//        assertThat(response.monthlySchedules()).hasSize(2);
//        assertThat(response.upcomingEvents()).hasSize(1);
//        assertThat(response.upcomingEvents().get(0).daysUntilEvent()).isEqualTo(3);
//
//        verify(scheduleRepository).findMonthlySchedules(eq(List.of(1L, 2L)), any(), any());
//        verify(scheduleRepository).findUpcomingEvents(eq(List.of(1L, 2L)), any(), any(Pageable.class));
//    }
//
//    /* ======================
//       통합 조회 - 특정 아티스트
//       ====================== */
//
//    @Test
//    @DisplayName("통합 조회: 특정 아티스트 필터링 성공")
//    void getIntegratedSchedules_SpecificArtist_Success() {
//        Long userId = 1L;
//        Long artistId = 1L;
//
//        User user = mock(User.class);
//        Artist artist = createArtist(artistId, "BTS");
//
//        given(userRepository.getReferenceById(userId)).willReturn(user);
//        given(artistRepository.getReferenceById(artistId)).willReturn(artist);
//        given(artistFollowRepository.existsByUserAndArtist(user, artist))
//                .willReturn(true);
//
//        given(scheduleRepository.findMonthlySchedules(anyList(), any(), any()))
//                .willReturn(Collections.emptyList());
//        given(scheduleRepository.findUpcomingEvents(anyList(), any(), any()))
//                .willReturn(Collections.emptyList());
//
//        scheduleService.getFollowingSchedules(userId, 2025, 12, Optional.of(artistId));
//
//        verify(artistFollowRepository, never()).findAllByUser(any());
//        verify(scheduleRepository).findMonthlySchedules(eq(List.of(artistId)), any(), any());
//        verify(scheduleRepository).findUpcomingEvents(eq(List.of(artistId)), any(), any());
//    }
//
//    @Test
//    @DisplayName("통합 조회 실패: 팔로우하지 않은 아티스트")
//    void getIntegratedSchedules_Fail_NotFollowed() {
//        Long userId = 1L;
//        Long artistId = 99L;
//
//        User user = mock(User.class);
//        Artist artist = createArtist(artistId, "Unknown");
//
//        given(userRepository.getReferenceById(userId)).willReturn(user);
//        given(artistRepository.getReferenceById(artistId)).willReturn(artist);
//        given(artistFollowRepository.existsByUserAndArtist(user, artist))
//                .willReturn(false);
//
//        assertThatThrownBy(() ->
//                scheduleService.getFollowingSchedules(userId, 2025, 12, Optional.of(artistId))
//        )
//                .isInstanceOf(ServiceException.class)
//                .hasMessageContaining(ErrorCode.ARTIST_NOT_FOLLOWED.getMessage());
//
//        verify(scheduleRepository, never()).findMonthlySchedules(any(), any(), any());
//    }
//
//    @Test
//    @DisplayName("통합 조회 실패: 잘못된 월 입력")
//    void getIntegratedSchedules_Fail_InvalidMonth() {
//        assertThatThrownBy(() ->
//                scheduleService.getFollowingSchedules(1L, 2025, 13, Optional.empty())
//        )
//                .isInstanceOf(ServiceException.class)
//                .hasMessageContaining(ErrorCode.INVALID_INPUT_VALUE.getMessage());
//    }
//
//    @Test
//    @DisplayName("통합 조회: 팔로우한 아티스트 없음")
//    void getIntegratedSchedules_NoFollow() {
//        Long userId = 1L;
//        User user = mock(User.class);
//
//        given(userRepository.getReferenceById(userId)).willReturn(user);
//        given(artistFollowRepository.findAllByUser(user))
//                .willReturn(Collections.emptyList());
//
//        FollowingSchedulesListResponse response =
//                scheduleService.getFollowingSchedules(userId, 2025, 12, Optional.empty());
//
//        assertThat(response.monthlySchedules()).isEmpty();
//        assertThat(response.upcomingEvents()).isEmpty();
//
//        verify(scheduleRepository, never()).findMonthlySchedules(any(), any(), any());
//    }
//
//    /* ======================
//       이벤트 리스트 조회
//       ====================== */
//
//    @Test
//    @DisplayName("파티 이벤트 리스트 조회 성공")
//    void getEventLists_Success() {
//        Long userId = 1L;
//        User user = mock(User.class);
//        Artist artist = createArtist(10L, "BTS");
//
//        given(userRepository.getReferenceById(userId)).willReturn(user);
//        given(artistFollowRepository.findAllByUser(user))
//                .willReturn(List.of(new ArtistFollow(user, artist)));
//
//        given(scheduleRepository.findPartyAvailableEvents(anyList(), anyList(), any(), any()))
//                .willReturn(List.of(new EventResponse(1L, "[BTS] 콘서트")));
//
//        EventsListResponse response = scheduleService.getEventLists(userId);
//
//        assertThat(response.events()).hasSize(1);
//        assertThat(response.events().get(0).title()).contains("BTS");
//    }
//
//    /* ======================
//       테스트 헬퍼
//       ====================== */
//
//    private Artist createArtist(Long id, String name) {
//        Artist artist = new Artist(name, "img");
//        ReflectionTestUtils.setField(artist, "id", id);
//        return artist;
//    }
//
//    private ScheduleResponse createScheduleDto(Long id, Long artistId, String artistName) {
//        return new ScheduleResponse(
//                id,
//                artistId,
//                artistName,
//                "콘서트",
//                ScheduleCategory.CONCERT,
//                LocalDateTime.now().plusDays(5),
//                null,
//                "https://ticket.com",
//                "서울"
//        );
//    }
//
//    private UpcomingEventResponse createUpcomingDtoRaw(Long id, String artistName, LocalDateTime time) {
//        return new UpcomingEventResponse(
//                id,
//                artistName,
//                "연말 무대",
//                ScheduleCategory.CONCERT,
//                time,
//                null,
//                "https://ticket.com",
//                null,
//                "서울"
//        );
//    }
//}
