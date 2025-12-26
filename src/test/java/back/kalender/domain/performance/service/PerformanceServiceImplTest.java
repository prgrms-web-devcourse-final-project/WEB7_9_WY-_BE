package back.kalender.domain.performance.service;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.artist.repository.ArtistRepository;
import back.kalender.domain.performance.performance.service.PerformanceService;
import back.kalender.domain.performance.performanceHall.entity.PerformanceHall;
import back.kalender.domain.performance.performanceHall.repository.PerformanceHallRepository;
import back.kalender.domain.performance.performance.dto.response.PerformanceDetailResponse;
import back.kalender.domain.performance.performance.entity.Performance;
import back.kalender.domain.performance.performance.repository.PerformanceRepository;
import back.kalender.domain.performance.schedule.repository.PerformanceScheduleRepository;
import back.kalender.domain.performance.priceGrade.repository.PriceGradeRepository;
import back.kalender.domain.performance.priceGrade.entity.PriceGrade;
import back.kalender.domain.performance.schedule.entity.PerformanceSchedule;
import back.kalender.domain.performance.schedule.entity.ScheduleStatus;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PerformanceService 단위 테스트")
class PerformanceServiceImplTest {

    @Mock
    private PerformanceRepository performanceRepository;

    @Mock
    private PriceGradeRepository priceGradeRepository;

    @Mock
    private PerformanceScheduleRepository performanceScheduleRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private PerformanceHallRepository performanceHallRepository;

    @InjectMocks
    private PerformanceService performanceService;

    private Performance performance;
    private PerformanceHall performanceHall;
    private Artist artist;
    private List<PriceGrade> priceGrades;
    private List<PerformanceSchedule> schedules;
    private List<LocalDate> availableDates;

    @BeforeEach
    void setUp() throws Exception {
        performanceHall = new PerformanceHall(
                "김대중컨벤션센터",
                "광주광역시 서구 내방로 111",
                "지하철 1호선 김대중컨벤션센터역 3번 출구"
        );
        setId(performanceHall, 1L);

        artist = new Artist("임영웅", "https://example.com/artist.jpg");
        setId(artist, 1L);

        performance = new Performance(
                performanceHall.getId(),
                artist.getId(),
                "임영웅 IM HERO TOUR 2025 - 광주",
                "https://example.com/poster.jpg",
                LocalDate.of(2026, 1, 3),
                LocalDate.of(2026, 1, 4),
                150,
                "※ 11/24(월)~28(금) 일괄배송 예정입니다.",
                LocalDateTime.of(2025, 9, 23, 14, 0),
                LocalDateTime.of(2026, 1, 4, 23, 59)
        );
        setId(performance, 1L);

        priceGrades = Arrays.asList(
                new PriceGrade(performance.getId(), "LOVE석", 176000),
                new PriceGrade(performance.getId(), "PEACE석", 154000)
        );

        PerformanceSchedule schedule1 = new PerformanceSchedule(
                performance.getId(),
                LocalDate.of(2026, 1, 3),
                LocalTime.of(18, 0),
                1,
                ScheduleStatus.AVAILABLE
        );
        setId(schedule1, 1L);

        PerformanceSchedule schedule2 = new PerformanceSchedule(
                performance.getId(),
                LocalDate.of(2026, 1, 4),
                LocalTime.of(14, 0),
                1,
                ScheduleStatus.SOLD_OUT
        );
        setId(schedule2, 2L);

        schedules = Arrays.asList(schedule1, schedule2);

        availableDates = Arrays.asList(
                LocalDate.of(2026, 1, 3),
                LocalDate.of(2026, 1, 4)
        );
    }

    private void setId(Object entity, Long id) throws Exception {
        var field = entity.getClass().getSuperclass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    @Test
    @DisplayName("공연 상세 조회 성공")
    void getPerformanceDetail_Success() {
        Long performanceId = 1L;

        given(performanceRepository.findById(performanceId))
                .willReturn(Optional.of(performance));
        given(performanceHallRepository.findById(performance.getPerformanceHallId()))
                .willReturn(Optional.of(performanceHall));
        given(artistRepository.findById(performance.getArtistId()))
                .willReturn(Optional.of(artist));
        given(priceGradeRepository.findAllByPerformanceId(performanceId))
                .willReturn(priceGrades);
        given(performanceScheduleRepository.findAvailableDatesByPerformanceId(performanceId))
                .willReturn(availableDates);
        given(performanceScheduleRepository.findAllByPerformanceIdOrderByPerformanceDateAscStartTimeAsc(performanceId))
                .willReturn(schedules);

        PerformanceDetailResponse response = performanceService.getPerformanceDetail(performanceId);

        assertThat(response.performanceId()).isEqualTo(1L);
        assertThat(response.schedules().get(0).status())
                .isEqualTo("AVAILABLE");

        assertThat(response.schedules().get(1).status())
                .isEqualTo("SOLD_OUT");


    }

    @Test
    @DisplayName("존재하지 않는 공연 조회시 예외")
    void getPerformanceDetail_NotFound() {
        given(performanceRepository.findById(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> performanceService.getPerformanceDetail(999L))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PERFORMANCE_NOT_FOUND);
    }

    @Test
    @DisplayName("다양한 가격 등급 포함 공연 조회")
    void getPerformanceDetail_MultiplePriceGrades() {
        Long performanceId = 1L;

        List<PriceGrade> multipleGrades = Arrays.asList(
                new PriceGrade(performance.getId(), "VIP석", 200000),
                new PriceGrade(performance.getId(), "R석", 150000),
                new PriceGrade(performance.getId(), "S석", 100000),
                new PriceGrade(performance.getId(), "A석", 70000)
        );

        given(performanceRepository.findById(performanceId))
                .willReturn(Optional.of(performance));
        given(performanceHallRepository.findById(performance.getPerformanceHallId()))
                .willReturn(Optional.of(performanceHall));
        given(artistRepository.findById(performance.getArtistId()))
                .willReturn(Optional.of(artist));
        given(priceGradeRepository.findAllByPerformanceId(performanceId))
                .willReturn(multipleGrades);
        given(performanceScheduleRepository.findAvailableDatesByPerformanceId(performanceId))
                .willReturn(availableDates);
        given(performanceScheduleRepository.findAllByPerformanceIdOrderByPerformanceDateAscStartTimeAsc(performanceId))
                .willReturn(schedules);

        PerformanceDetailResponse response = performanceService.getPerformanceDetail(performanceId);

        assertThat(response.priceGrades()).hasSize(4);
    }

    @Test
    @DisplayName("모든 회차가 매진일 때")
    void getPerformanceDetail_AllSoldOut() {
        Long performanceId = 1L;

        List<PerformanceSchedule> soldOutSchedules = Arrays.asList(
                new PerformanceSchedule(performance.getId(), LocalDate.of(2026,1,3), LocalTime.of(18,0), 1, ScheduleStatus.SOLD_OUT),
                new PerformanceSchedule(performance.getId(), LocalDate.of(2026,1,4), LocalTime.of(14,0), 1, ScheduleStatus.SOLD_OUT)
        );

        given(performanceRepository.findById(performanceId))
                .willReturn(Optional.of(performance));
        given(performanceHallRepository.findById(performance.getPerformanceHallId()))
                .willReturn(Optional.of(performanceHall));

        given(artistRepository.findById(performance.getArtistId()))
                .willReturn(Optional.of(artist));
        given(priceGradeRepository.findAllByPerformanceId(performanceId))
                .willReturn(priceGrades);
        given(performanceScheduleRepository.findAvailableDatesByPerformanceId(performanceId))
                .willReturn(List.of());
        given(performanceScheduleRepository.findAllByPerformanceIdOrderByPerformanceDateAscStartTimeAsc(performanceId))
                .willReturn(soldOutSchedules);

        PerformanceDetailResponse response = performanceService.getPerformanceDetail(performanceId);
        assertThat(response.schedules())
                .allMatch(s -> s.status().equals("SOLD_OUT"));

    }

    @Test
    @DisplayName("일부 회차만 예매 가능")
    void getPerformanceDetail_PartiallyAvailable() {
        Long performanceId = 1L;

        List<PerformanceSchedule> mixed = Arrays.asList(
                new PerformanceSchedule(performance.getId(), LocalDate.of(2026,1,3), LocalTime.of(14,0), 1, ScheduleStatus.SOLD_OUT),
                new PerformanceSchedule(performance.getId(), LocalDate.of(2026,1,3), LocalTime.of(18,0), 2, ScheduleStatus.AVAILABLE)
        );

        given(performanceRepository.findById(performanceId))
                .willReturn(Optional.of(performance));
        given(performanceHallRepository.findById(any()))
                .willReturn(Optional.of(performanceHall));
        given(artistRepository.findById(any()))
                .willReturn(Optional.of(artist));
        given(priceGradeRepository.findAllByPerformanceId(any()))
                .willReturn(priceGrades);
        given(performanceScheduleRepository.findAvailableDatesByPerformanceId(any()))
                .willReturn(List.of(LocalDate.of(2026,1,3)));
        given(performanceScheduleRepository.findAllByPerformanceIdOrderByPerformanceDateAscStartTimeAsc(any()))
                .willReturn(mixed);

        PerformanceDetailResponse response = performanceService.getPerformanceDetail(performanceId);

        assertThat(response.schedules().get(0).status())
                .isEqualTo("SOLD_OUT");

        assertThat(response.schedules().get(1).status())
                .isEqualTo("AVAILABLE");
    }
}
