package back.kalender.domain.booking.reservation.service;

import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
import back.kalender.domain.booking.reservation.dto.response.MyReservationListResponse;
import back.kalender.domain.booking.reservation.dto.response.ReservationDetailResponse;
import back.kalender.domain.booking.reservation.entity.Reservation;
import back.kalender.domain.booking.reservation.entity.ReservationStatus;
import back.kalender.domain.booking.reservation.mapper.ReservationMapper;
import back.kalender.domain.booking.reservation.repository.ReservationRepository;
import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
import back.kalender.domain.booking.reservationSeat.repository.ReservationSeatRepository;
import back.kalender.domain.performance.performance.entity.Performance;
import back.kalender.domain.performance.performance.repository.PerformanceRepository;
import back.kalender.domain.performance.performanceHall.entity.PerformanceHall;
import back.kalender.domain.performance.performanceHall.repository.PerformanceHallRepository;
import back.kalender.domain.performance.priceGrade.entity.PriceGrade;
import back.kalender.domain.performance.priceGrade.repository.PriceGradeRepository;
import back.kalender.domain.performance.schedule.entity.PerformanceSchedule;
import back.kalender.domain.performance.schedule.entity.ScheduleStatus;
import back.kalender.domain.performance.schedule.repository.PerformanceScheduleRepository;
import back.kalender.global.exception.ErrorCode;
import back.kalender.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService 마이페이지 테스트")
public class ReservationServiceMyPageTest {
    @InjectMocks
    private ReservationService reservationService;

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ReservationSeatRepository reservationSeatRepository;
    @Mock
    private PerformanceScheduleRepository scheduleRepository;
    @Mock
    private PerformanceRepository performanceRepository;
    @Mock
    private PerformanceHallRepository hallRepository;
    @Mock
    private PerformanceSeatRepository performanceSeatRepository;
    @Mock
    private PriceGradeRepository priceGradeRepository;
    @Mock
    private ReservationMapper reservationMapper;

    private static final Long USER_ID = 1L;
    private static final Long RESERVATION_ID = 1L;
    private static final Long SCHEDULE_ID = 1L;
    private static final Long PERFORMANCE_ID = 1L;
    private static final Long HALL_ID = 1L;
    private static final Long SEAT_ID = 1L;
    private static final Long PRICE_GRADE_ID = 1L;

    private Reservation reservation;
    private PerformanceSchedule schedule;
    private Performance performance;
    private PerformanceHall hall;
    private PriceGrade priceGrade;
    private PerformanceSeat performanceSeat;
    private ReservationSeat reservationSeat;

    @BeforeEach
    void setUp() {
        hall = new PerformanceHall(
                "김대중컨벤션센터",
                "광주광역시 서구 내방로 111",
                "지하철 1호선"
        );
        ReflectionTestUtils.setField(hall, "id", HALL_ID);

        performance = new Performance(
                HALL_ID,
                1L,
                "임영웅 IM HERO TOUR 2025",
                "https://example.com/poster.jpg",
                LocalDate.of(2026, 1, 3),
                LocalDate.of(2026, 1, 4),
                150,
                "예매 안내",
                LocalDateTime.of(2025, 9, 23, 14, 0),
                LocalDateTime.of(2026, 1, 4, 23, 59)
        );
        ReflectionTestUtils.setField(performance, "id", PERFORMANCE_ID);

        schedule = new PerformanceSchedule(
                PERFORMANCE_ID,
                LocalDate.of(2026, 1, 3),
                LocalTime.of(18, 0),
                1,
                ScheduleStatus.AVAILABLE
        );
        ReflectionTestUtils.setField(schedule, "id", SCHEDULE_ID);

        priceGrade = new PriceGrade(PERFORMANCE_ID, "VIP", 200_000);
        ReflectionTestUtils.setField(priceGrade, "id", PRICE_GRADE_ID);

        performanceSeat = PerformanceSeat.create(
                SCHEDULE_ID, 1L, PRICE_GRADE_ID, 1, "A", 1, 1, 0, 1000
        );
        ReflectionTestUtils.setField(performanceSeat, "id", SEAT_ID);

        reservationSeat = new ReservationSeat(RESERVATION_ID, SEAT_ID, 200_000);
        ReflectionTestUtils.setField(reservationSeat, "id", 1L);

        reservation = Reservation.builder()
                .userId(USER_ID)
                .performanceScheduleId(SCHEDULE_ID)
                .status(ReservationStatus.PAID)
                .totalAmount(200_000)
                .build();
        ReflectionTestUtils.setField(reservation, "id", RESERVATION_ID);
    }

    @Nested
    @DisplayName("getMyReservations 테스트")
    class GetMyReservationsTest {

        @Test
        @DisplayName("성공: 예매 내역 목록 조회")
        void getMyReservations_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Reservation> reservationPage = new PageImpl<>(
                    List.of(reservation),
                    pageable,
                    1
            );

            given(reservationRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(
                    eq(USER_ID),
                    anyList(),
                    eq(pageable)
            )).willReturn(reservationPage);

            given(scheduleRepository.findAllById(anySet()))
                    .willReturn(List.of(schedule));
            given(performanceRepository.findAllById(anySet()))
                    .willReturn(List.of(performance));
            given(hallRepository.findAllById(anySet()))
                    .willReturn(List.of(hall));

            Object[] seatCountArray = new Object[]{RESERVATION_ID, 3L};
            List<Object[]> seatCounts = new ArrayList<>();
            seatCounts.add(seatCountArray);

            given(reservationSeatRepository.countByReservationIds(anyList()))
                    .willReturn(seatCounts);

            MyReservationListResponse.ReservationItem mockItem =
                    new MyReservationListResponse.ReservationItem(
                            RESERVATION_ID,
                            "M1",
                            "임영웅 IM HERO TOUR 2025",
                            "김대중컨벤션센터",
                            LocalDate.of(2026, 1, 3),
                            LocalTime.of(18, 0),
                            "3매",
                            "예매완료(카카오페이)",
                            "PAID",
                            "2026-01-03 17:00까지",
                            LocalDateTime.now()
                    );

            given(reservationMapper.toMyReservationItem(
                    any(Reservation.class),
                    any(PerformanceSchedule.class),
                    any(Performance.class),
                    any(PerformanceHall.class),
                    anyInt()
            )).willReturn(mockItem);

            MyReservationListResponse response = reservationService.getMyReservations(
                    USER_ID, pageable
            );

            assertThat(response).isNotNull();
            assertThat(response.reservations()).hasSize(1);
            assertThat(response.currentPage()).isEqualTo(0);
            assertThat(response.totalPages()).isEqualTo(1);
            assertThat(response.totalElements()).isEqualTo(1);

            MyReservationListResponse.ReservationItem item = response.reservations().get(0);
            assertThat(item.reservationId()).isEqualTo(RESERVATION_ID);
            assertThat(item.performanceTitle()).isEqualTo("임영웅 IM HERO TOUR 2025");
            assertThat(item.status()).isEqualTo("PAID");
        }

        @Test
        @DisplayName("성공: 빈 목록 조회")
        void getMyReservations_Success_EmptyList() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Reservation> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(reservationRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(
                    eq(USER_ID),
                    anyList(),
                    eq(pageable)
            )).willReturn(emptyPage);

            MyReservationListResponse response = reservationService.getMyReservations(
                    USER_ID, pageable
            );

            assertThat(response).isNotNull();
            assertThat(response.reservations()).isEmpty();
            assertThat(response.totalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("성공: 페이징 처리")
        void getMyReservations_Success_Paging() {
            Pageable pageable = PageRequest.of(1, 5);  // 2페이지

            Reservation reservation2 = Reservation.builder()
                    .userId(USER_ID)
                    .performanceScheduleId(SCHEDULE_ID)
                    .status(ReservationStatus.PAID)
                    .totalAmount(400_000)
                    .build();
            ReflectionTestUtils.setField(reservation2, "id", 2L);

            Page<Reservation> reservationPage = new PageImpl<>(
                    List.of(reservation2),
                    pageable,
                    10  // 총 10개
            );

            given(reservationRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(
                    eq(USER_ID),
                    anyList(),
                    eq(pageable)
            )).willReturn(reservationPage);

            given(scheduleRepository.findAllById(anySet()))
                    .willReturn(List.of(schedule));
            given(performanceRepository.findAllById(anySet()))
                    .willReturn(List.of(performance));
            given(hallRepository.findAllById(anySet()))
                    .willReturn(List.of(hall));

            Object[] seatCountArray2 = new Object[]{2L, 2L};
            List<Object[]> seatCountsForPage2 = new ArrayList<>();
            seatCountsForPage2.add(seatCountArray2);
            given(reservationSeatRepository.countByReservationIds(anyList()))
                    .willReturn(seatCountsForPage2);

            MyReservationListResponse.ReservationItem mockItem =
                    new MyReservationListResponse.ReservationItem(
                            2L, "M2", "공연", "홀", LocalDate.now(), LocalTime.now(),
                            "2매", "예매완료", "PAID", null, LocalDateTime.now()
                    );

            given(reservationMapper.toMyReservationItem(any(), any(), any(), any(), anyInt()))
                    .willReturn(mockItem);

            MyReservationListResponse response = reservationService.getMyReservations(
                    USER_ID, pageable
            );

            assertThat(response.currentPage()).isEqualTo(1);
            assertThat(response.totalPages()).isEqualTo(2);  // 10개 / 5개 = 2페이지
            assertThat(response.totalElements()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("getReservationDetail 테스트")
    class GetReservationDetailTest {

        @Test
        @DisplayName("성공: 예매 상세 조회")
        void getReservationDetail_Success() {
            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));
            given(scheduleRepository.findById(SCHEDULE_ID))
                    .willReturn(Optional.of(schedule));
            given(performanceRepository.findById(PERFORMANCE_ID))
                    .willReturn(Optional.of(performance));
            given(hallRepository.findById(HALL_ID))
                    .willReturn(Optional.of(hall));
            given(reservationSeatRepository.findByReservationId(RESERVATION_ID))
                    .willReturn(List.of(reservationSeat));
            given(performanceSeatRepository.findAllById(anyList()))
                    .willReturn(List.of(performanceSeat));
            given(priceGradeRepository.findAllById(anySet()))
                    .willReturn(List.of(priceGrade));

            ReservationDetailResponse mockResponse = new ReservationDetailResponse(
                    new ReservationDetailResponse.ReservationInfo(
                            RESERVATION_ID,
                            "M1",
                            "PAID",
                            "예매완료(카카오페이)",
                            LocalDateTime.now(),
                            true,
                            LocalDateTime.of(2026, 1, 3, 17, 0)
                    ),
                    new ReservationDetailResponse.PerformanceInfo(
                            PERFORMANCE_ID,
                            "임영웅 IM HERO TOUR 2025",
                            "김대중컨벤션센터",
                            LocalDate.of(2026, 1, 3),
                            LocalTime.of(18, 0),
                            "1회차"
                    ),
                    List.of(new ReservationDetailResponse.SeatInfo(
                            SEAT_ID, "1층", "A블록", "1열", "1번", "VIP", 200_000
                    )),
                    new ReservationDetailResponse.PaymentInfo(
                            200_000, "카카오페이", LocalDateTime.now(), "TODO"
                    ),
                    new ReservationDetailResponse.DeliveryInfo(
                            "배송", "홍길동", "010-1234-5678", "서울시 강남구"
                    )
            );

            given(reservationMapper.toReservationDetailResponse(
                    any(), any(), any(), any(), anyList(), anyMap(), anyMap()
            )).willReturn(mockResponse);

            ReservationDetailResponse response = reservationService.getReservationDetail(
                    RESERVATION_ID, USER_ID
            );

            assertThat(response).isNotNull();
            assertThat(response.reservationInfo().reservationId()).isEqualTo(RESERVATION_ID);
            assertThat(response.performanceInfo().title()).isEqualTo("임영웅 IM HERO TOUR 2025");
            assertThat(response.seats()).hasSize(1);
            assertThat(response.paymentInfo().totalAmount()).isEqualTo(200_000);
        }

        @Test
        @DisplayName("실패: 예매를 찾을 수 없음")
        void getReservationDetail_Fail_ReservationNotFound() {
            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.getReservationDetail(
                    RESERVATION_ID, USER_ID
            ))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 권한 없음 (다른 사용자)")
        void getReservationDetail_Fail_Unauthorized() {
            Long otherUserId = 999L;
            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.getReservationDetail(
                    RESERVATION_ID, otherUserId
            ))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
        }

        @Test
        @DisplayName("실패: 스케줄을 찾을 수 없음")
        void getReservationDetail_Fail_ScheduleNotFound() {
            given(reservationRepository.findById(RESERVATION_ID))
                    .willReturn(Optional.of(reservation));
            given(scheduleRepository.findById(SCHEDULE_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.getReservationDetail(
                    RESERVATION_ID, USER_ID
            ))
                    .isInstanceOf(ServiceException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
        }
    }
}
