package back.kalender.domain.performance;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.performance.dto.response.PerformanceDetailResponse;
import back.kalender.domain.performance.entity.*;
import back.kalender.domain.performance.repository.PerformanceRepository;
import back.kalender.domain.performance.repository.PerformanceScheduleRepository;
import back.kalender.domain.performance.repository.PriceGradeRepository;
import back.kalender.domain.performance.service.PerformanceServiceImpl;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PerformanceService 단위 테스트")
class PerformanceServiceImplTest {

    @Mock
    private PerformanceRepository performanceRepository;

    @Mock
    private PriceGradeRepository priceGradeRepository;

    @Mock
    private PerformanceScheduleRepository performanceScheduleRepository;

    @InjectMocks
    private PerformanceServiceImpl performanceService;

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

        artist = new Artist(
                "임영웅",
                "https://example.com/artist.jpg"
        );
        setId(artist, 1L);

        performance = new Performance(
                performanceHall,
                artist,
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

        PriceGrade priceGrade1 = new PriceGrade(performance, "LOVE석", 176000);
        setId(priceGrade1, 1L);

        PriceGrade priceGrade2 = new PriceGrade(performance, "PEACE석", 154000);
        setId(priceGrade2, 2L);

        priceGrades = Arrays.asList(priceGrade1, priceGrade2);

        PerformanceSchedule schedule1 = new PerformanceSchedule(
                performance,
                LocalDate.of(2026, 1, 3),
                LocalTime.of(18, 0),
                1,
                150,
                200,
                ScheduleStatus.AVAILABLE
        );
        setId(schedule1, 1L);

        PerformanceSchedule schedule2 = new PerformanceSchedule(
                performance,
                LocalDate.of(2026, 1, 4),
                LocalTime.of(14, 0),
                1,
                0,
                200,
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
        java.lang.reflect.Field field = entity.getClass().getSuperclass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    @Test
    @DisplayName("공연 상세 조회 성공")
    void getPerformanceDetail_Success() {
        Long performanceId = 1L;
        given(performanceRepository.findByIdWithDetails(performanceId))
                .willReturn(Optional.of(performance));
        given(priceGradeRepository.findAllByPerformance(performance))
                .willReturn(priceGrades);
        given(performanceScheduleRepository.findAvailableDatesByPerformance(performance))
                .willReturn(availableDates);
        given(performanceScheduleRepository.findAllByPerformanceOrderByPerformanceDateAscStartTimeAsc(performance))
                .willReturn(schedules);

        PerformanceDetailResponse response = performanceService.getPerformanceDetail(performanceId);

        assertThat(response).isNotNull();
        assertThat(response.performanceId()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("임영웅 IM HERO TOUR 2025 - 광주");

        // 아티스트 정보 검증
        assertThat(response.artist().artistId()).isEqualTo(1L);
        assertThat(response.artist().artistName()).isEqualTo("임영웅");

        // 공연장 정보 검증
        assertThat(response.performanceHall().performanceHallId()).isEqualTo(1L);
        assertThat(response.performanceHall().name()).isEqualTo("김대중컨벤션센터");

        // 가격 등급 검증
        assertThat(response.priceGrades()).hasSize(2);
        assertThat(response.priceGrades().get(0).gradeName()).isEqualTo("LOVE석");
        assertThat(response.priceGrades().get(0).price()).isEqualTo(176000);

        // 회차 정보 검증
        assertThat(response.schedules()).hasSize(2);
        assertThat(response.schedules().get(0).isBookingAvailable()).isTrue();
        assertThat(response.schedules().get(1).isBookingAvailable()).isFalse();

        // 예매 가능 날짜 검증
        assertThat(response.availableDates()).hasSize(2);
        assertThat(response.availableDates()).contains(
                LocalDate.of(2026, 1, 3),
                LocalDate.of(2026, 1, 4)
        );

        verify(performanceRepository, times(1)).findByIdWithDetails(performanceId);
        verify(priceGradeRepository, times(1)).findAllByPerformance(performance);
        verify(performanceScheduleRepository, times(1)).findAvailableDatesByPerformance(performance);
        verify(performanceScheduleRepository, times(1))
                .findAllByPerformanceOrderByPerformanceDateAscStartTimeAsc(performance);
    }

    @Test
    @DisplayName("존재하지 않는 공연 ID로 조회 시 예외 발생")
    void getPerformanceDetail_NotFound() {
        Long invalidPerformanceId = 999L;
        given(performanceRepository.findByIdWithDetails(invalidPerformanceId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> performanceService.getPerformanceDetail(invalidPerformanceId))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PERFORMANCE_NOT_FOUND);

        verify(performanceRepository, times(1)).findByIdWithDetails(invalidPerformanceId);
    }

    @Test
    @DisplayName("다양한 가격 등급이 있는 공연 조회")
    void getPerformanceDetail_MultiplePriceGrades() {
        Long performanceId = 1L;

        // 4개의 가격 등급
        PriceGrade grade1 = new PriceGrade(performance, "VIP석", 200000);
        PriceGrade grade2 = new PriceGrade(performance, "R석", 150000);
        PriceGrade grade3 = new PriceGrade(performance, "S석", 100000);
        PriceGrade grade4 = new PriceGrade(performance, "A석", 70000);

        List<PriceGrade> multiplePriceGrades = Arrays.asList(grade1, grade2, grade3, grade4);

        given(performanceRepository.findByIdWithDetails(performanceId))
                .willReturn(Optional.of(performance));
        given(priceGradeRepository.findAllByPerformance(performance))
                .willReturn(multiplePriceGrades);
        given(performanceScheduleRepository.findAvailableDatesByPerformance(performance))
                .willReturn(availableDates);
        given(performanceScheduleRepository.findAllByPerformanceOrderByPerformanceDateAscStartTimeAsc(performance))
                .willReturn(schedules);

        PerformanceDetailResponse response = performanceService.getPerformanceDetail(performanceId);

        assertThat(response).isNotNull();
        assertThat(response.priceGrades()).hasSize(4);
        assertThat(response.priceGrades())
                .extracting("gradeName")
                .containsExactly("VIP석", "R석", "S석", "A석");
        assertThat(response.priceGrades())
                .extracting("price")
                .containsExactly(200000, 150000, 100000, 70000);
    }

    @Test
    @DisplayName("모든 회차가 매진인 공연 조회")
    void getPerformanceDetail_AllSchedulesSoldOut() {
        Long performanceId = 1L;

        List<PerformanceSchedule> soldOutSchedules = Arrays.asList(
                new PerformanceSchedule(
                        performance,
                        LocalDate.of(2026, 1, 3),
                        LocalTime.of(18, 0),
                        1,
                        0,
                        200,
                        ScheduleStatus.SOLD_OUT
                ),
                new PerformanceSchedule(
                        performance,
                        LocalDate.of(2026, 1, 4),
                        LocalTime.of(14, 0),
                        1,
                        0,
                        200,
                        ScheduleStatus.SOLD_OUT
                )
        );

        given(performanceRepository.findByIdWithDetails(performanceId))
                .willReturn(Optional.of(performance));
        given(priceGradeRepository.findAllByPerformance(performance))
                .willReturn(priceGrades);
        given(performanceScheduleRepository.findAvailableDatesByPerformance(performance))
                .willReturn(List.of()); // 예매 가능한 날짜 없음
        given(performanceScheduleRepository.findAllByPerformanceOrderByPerformanceDateAscStartTimeAsc(performance))
                .willReturn(soldOutSchedules);

        PerformanceDetailResponse response = performanceService.getPerformanceDetail(performanceId);

        assertThat(response).isNotNull();
        assertThat(response.schedules()).hasSize(2);
        assertThat(response.schedules()).allMatch(schedule -> !schedule.isBookingAvailable());
        assertThat(response.availableDates()).isEmpty(); // 예매 가능한 날짜 없음
    }

    @Test
    @DisplayName("일부 회차만 예매 가능한 공연 조회")
    void getPerformanceDetail_PartiallyAvailable() {
        Long performanceId = 1L;

        // 1회차는 매진, 2회차는 예매 가능
        List<PerformanceSchedule> mixedSchedules = Arrays.asList(
                new PerformanceSchedule(
                        performance,
                        LocalDate.of(2026, 1, 3),
                        LocalTime.of(14, 0),
                        1,
                        0,
                        200,
                        ScheduleStatus.SOLD_OUT
                ),
                new PerformanceSchedule(
                        performance,
                        LocalDate.of(2026, 1, 3),
                        LocalTime.of(18, 0),
                        2,
                        150,
                        200,
                        ScheduleStatus.AVAILABLE
                )
        );

        List<LocalDate> partialAvailableDates = List.of(LocalDate.of(2026, 1, 3));

        given(performanceRepository.findByIdWithDetails(performanceId))
                .willReturn(Optional.of(performance));
        given(priceGradeRepository.findAllByPerformance(performance))
                .willReturn(priceGrades);
        given(performanceScheduleRepository.findAvailableDatesByPerformance(performance))
                .willReturn(partialAvailableDates);
        given(performanceScheduleRepository.findAllByPerformanceOrderByPerformanceDateAscStartTimeAsc(performance))
                .willReturn(mixedSchedules);

        PerformanceDetailResponse response = performanceService.getPerformanceDetail(performanceId);

        assertThat(response).isNotNull();
        assertThat(response.schedules()).hasSize(2);

        // 1회차는 예매 불가, 2회차는 예매 가능
        assertThat(response.schedules().get(0).isBookingAvailable()).isFalse();
        assertThat(response.schedules().get(1).isBookingAvailable()).isTrue();

        // 예매 가능한 날짜는 1개 (1/3만)
        assertThat(response.availableDates()).hasSize(1);
        assertThat(response.availableDates().get(0)).isEqualTo(LocalDate.of(2026, 1, 3));
    }


    @Test
    @DisplayName("예매 오픈 전 - secondsUntilOpen 계산 검증 (24시간 이내)")
    void getPerformanceDetail_BeforeBookingOpen_Within24Hours() {
        Long performanceId = 1L;

        // 1시간 후에 예매 오픈되는 공연
        Performance futurePerformance = new Performance(
                performanceHall,
                artist,
                "임영웅 IM HERO TOUR 2025 - 광주",
                "https://example.com/poster.jpg",
                LocalDate.of(2026, 1, 3),
                LocalDate.of(2026, 1, 4),
                150,
                "※ 11/24(월)~28(금) 일괄배송 예정입니다.",
                LocalDateTime.now().plusHours(1), // 1시간 후
                LocalDateTime.of(2026, 1, 4, 23, 59)
        );

        given(performanceRepository.findByIdWithDetails(performanceId))
                .willReturn(Optional.of(futurePerformance));
        given(priceGradeRepository.findAllByPerformance(any()))
                .willReturn(priceGrades);
        given(performanceScheduleRepository.findAvailableDatesByPerformance(any()))
                .willReturn(availableDates);
        given(performanceScheduleRepository.findAllByPerformanceOrderByPerformanceDateAscStartTimeAsc(any()))
                .willReturn(schedules);

        PerformanceDetailResponse response = performanceService.getPerformanceDetail(performanceId);

        assertThat(response.isBookingOpen()).isFalse();
        assertThat(response.secondsUntilOpen()).isNotNull();
        assertThat(response.secondsUntilOpen()).isGreaterThan(0L);
        assertThat(response.secondsUntilOpen()).isLessThanOrEqualTo(86400L); // 24시간 이내
    }

    @Test
    @DisplayName("예매 오픈 전 - secondsUntilOpen null (24시간 초과)")
    void getPerformanceDetail_BeforeBookingOpen_Over24Hours() {
        Long performanceId = 1L;

        Performance futurePerformance = new Performance(
                performanceHall,
                artist,
                "임영웅 IM HERO TOUR 2025 - 광주",
                "https://example.com/poster.jpg",
                LocalDate.of(2026, 1, 3),
                LocalDate.of(2026, 1, 4),
                150,
                "※ 11/24(월)~28(금) 일괄배송 예정입니다.",
                LocalDateTime.now().plusDays(3), // 3일 후
                LocalDateTime.of(2026, 1, 4, 23, 59)
        );

        given(performanceRepository.findByIdWithDetails(performanceId))
                .willReturn(Optional.of(futurePerformance));
        given(priceGradeRepository.findAllByPerformance(any()))
                .willReturn(priceGrades);
        given(performanceScheduleRepository.findAvailableDatesByPerformance(any()))
                .willReturn(availableDates);
        given(performanceScheduleRepository.findAllByPerformanceOrderByPerformanceDateAscStartTimeAsc(any()))
                .willReturn(schedules);

        PerformanceDetailResponse response = performanceService.getPerformanceDetail(performanceId);

        assertThat(response.isBookingOpen()).isFalse();
        assertThat(response.secondsUntilOpen()).isNull(); // 24시간 초과라서 null
    }

    @Test
    @DisplayName("예매 오픈됨 - isBookingOpen true")
    void getPerformanceDetail_BookingOpened() {
        Long performanceId = 1L;

        // 이미 예매가 오픈된 공연
        Performance openedPerformance = new Performance(
                performanceHall,
                artist,
                "임영웅 IM HERO TOUR 2025 - 광주",
                "https://example.com/poster.jpg",
                LocalDate.of(2026, 1, 3),
                LocalDate.of(2026, 1, 4),
                150,
                "※ 11/24(월)~28(금) 일괄배송 예정입니다.",
                LocalDateTime.now().minusDays(1), // 1일 전에 오픈
                LocalDateTime.of(2026, 1, 4, 23, 59)
        );

        given(performanceRepository.findByIdWithDetails(performanceId))
                .willReturn(Optional.of(openedPerformance));
        given(priceGradeRepository.findAllByPerformance(any()))
                .willReturn(priceGrades);
        given(performanceScheduleRepository.findAvailableDatesByPerformance(any()))
                .willReturn(availableDates);
        given(performanceScheduleRepository.findAllByPerformanceOrderByPerformanceDateAscStartTimeAsc(any()))
                .willReturn(schedules);

        PerformanceDetailResponse response = performanceService.getPerformanceDetail(performanceId);

        assertThat(response.isBookingOpen()).isTrue();
        assertThat(response.secondsUntilOpen()).isNull();
    }
}